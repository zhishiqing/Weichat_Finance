package com.weichat.finance.job;

import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.PayOrder;
import com.weichat.finance.entity.PayTransaction;
import com.weichat.finance.entity.enums.OrderStatus;
import com.weichat.finance.entity.enums.PayStatus;
import com.weichat.finance.payment.config.WechatPayProperties;
import com.weichat.finance.payment.v3.jsapi.JsapiQueryResponse;
import com.weichat.finance.payment.v3.jsapi.JsapiService;
import com.weichat.finance.service.MerchantConfigService;
import com.weichat.finance.service.PayOrderService;
import com.weichat.finance.service.PayTransactionService;
import com.weichat.finance.trace.ScheduledTaskMdcHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 拉起支付后的主动轮询查单调度器（v1.4 引入）。
 *
 * <h3>背景</h3>
 * 微信支付回调地址暂不可配置，改为"拉起支付后主动轮询查单"策略。
 * 下单成功后立即入队（{@code next_query_at = NOW()}），调度器每 3 秒扫描一次。
 *
 * <h3>轮询策略</h3>
 * <ul>
 *   <li>首次入队：立即查（下一次调度执行时，~3s 内）</li>
 *   <li>NOTPAY 等未支付状态：固定 3 秒后再查（v1.5.1 用户要求"3s 一次"）</li>
 *   <li>SUCCESS / CLOSED / REFUNDED / REVOKED：停止轮询</li>
 *   <li>最多轮询：30 分钟（超时停止，移交兜底查单）</li>
 * </ul>
 *
 * <h3>终止条件</h3>
 * 支付成功（{@code PayStatus.SUCCESS}）/ 已关闭（{@code PayStatus.CLOSED}）/ 已退款（{@code PayStatus.REFUNDED}）
 *
 * <h3>与 {@link PayOrderQueryScheduler} 的关系</h3>
 * <ul>
 *   <li>{@link PayOrderQueryScheduler}：全表扫描兜底（30 分钟一次，处理漏查单）</li>
 *   <li>{@link PayOrderPollScheduler}：高频精准轮询（10 秒一次，处理"刚下完单等支付"）</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-16
 */
@Component
public class PayOrderPollScheduler {

    private static final Logger log = LoggerFactory.getLogger(PayOrderPollScheduler.class);

    /** 每批最大处理条数 */
    private static final int BATCH_SIZE = 50;

    /** 基础轮询间隔（秒） */
    private static final int BASE_INTERVAL_SECONDS = 3;

    /** 最大轮询次数（达到后停止轮询，移交兜底查单） */
    private static final int MAX_POLL_TIMES = 30;

    /** 最大轮询时长（分钟，超过后强制停止） */
    private static final int MAX_POLL_MINUTES = 30;

    @Autowired
    private PayOrderService payOrderService;

    @Autowired
    private PayTransactionService payTransactionService;

    @Autowired
    private MerchantConfigService merchantConfigService;

    @Autowired
    private WechatPayProperties wechatPayProperties;

    @Autowired(required = false)
    private JsapiService jsapiService;

    @Value("${scheduler.pay-order-poll.enabled:true}")
    private boolean enabled;

    /**
     * 每 10 秒执行一次扫描。
     */
    @Scheduled(fixedRateString = "${scheduler.pay-order-poll.interval-ms:3000}",
               initialDelayString = "${scheduler.pay-order-poll.initial-delay-ms:3000}")
    public void pollDueOrders() {
        if (!enabled) {
            return;
        }
        String traceId = ScheduledTaskMdcHelper.startScheduledTask("payOrderPoll");
        try {
            doPoll();
        } finally {
            ScheduledTaskMdcHelper.endScheduledTask();
        }
    }

    private void doPoll() {
        LocalDateTime now = LocalDateTime.now();
        List<PayOrder> orders = payOrderService.listDueForQuery(now, BATCH_SIZE);
        if (orders.isEmpty()) {
            return;
        }
        log.debug("[轮询查单] 扫描到 {} 笔到期订单", orders.size());

        // MOCK 模式：只推进 next_query_at（不实际查微信）
        if ("MOCK".equalsIgnoreCase(wechatPayProperties.getMode())) {
            for (PayOrder order : orders) {
                LocalDateTime nextAt = payOrderService.computeNextQueryTime(BASE_INTERVAL_SECONDS, 1);
                payOrderService.setNextQueryTime(order.getId(), nextAt);
            }
            return;
        }

        for (PayOrder order : orders) {
            processOrder(order);
        }
    }

    /**
     * 处理单笔订单：查单 → 同步状态 → 计算下次查询时间。
     */
    private void processOrder(PayOrder order) {
        String outTradeNo = order.getOutTradeNo();
        LocalDateTime gmtCreate = order.getGmtCreate();

        // 1. 超时检查：已超过最大轮询时长
        if (gmtCreate != null
            && LocalDateTime.now().isAfter(gmtCreate.plusMinutes(MAX_POLL_MINUTES))) {
            log.info("[轮询查单] 订单超过最大轮询时长 {} 分钟，停止轮询: outTradeNo={}",
                MAX_POLL_MINUTES, outTradeNo);
            payOrderService.setNextQueryTime(order.getId(), null);
            return;
        }

        // 1.5 获取真实商户配置（避免传 null 给 SDK）
        MerchantConfig merchant = merchantConfigService.getByMchId(order.getMchId());
        if (merchant == null) {
            log.error("[轮询查单] 查不到商户配置: mchId={}", order.getMchId());
            return;
        }

        // 2. 调微信查单
        JsapiQueryResponse resp = null;
        Exception lastException = null;
        for (int retry = 0; retry < 2; retry++) {
            try {
                resp = jsapiService.queryByOutTradeNo(outTradeNo, merchant);
                break;
            } catch (Exception e) {
                lastException = e;
                log.warn("[轮询查单] 查单失败 outTradeNo={}, 重试 {}/2: {}",
                    outTradeNo, retry + 1, e.getMessage());
                if (retry == 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        if (resp == null) {
            log.error("[轮询查单] 查单最终失败: outTradeNo={}, error={}",
                outTradeNo, lastException != null ? lastException.getMessage() : "未知");
            // 失败也推进 next_query_at（避免死循环），固定 3 秒
            LocalDateTime nextAt = LocalDateTime.now().plusSeconds(BASE_INTERVAL_SECONDS);
            payOrderService.setNextQueryTime(order.getId(), nextAt);
            return;
        }

        String payStatus = resp.getPayStatus();
        log.info("[轮询查单] outTradeNo={} → payStatus={}", outTradeNo, payStatus);

        // 3. 终态：停止轮询
        if (PayStatus.SUCCESS.equals(payStatus)
            || PayStatus.CLOSED.equals(payStatus)
            || PayStatus.REFUNDED.equals(payStatus)
            || PayStatus.REVOKED.equals(payStatus)) {

            syncOrderStatus(order, resp);
            payOrderService.setNextQueryTime(order.getId(), null);
            log.info("[轮询查单] ✅ 终态到达，停止轮询: outTradeNo={}, payStatus={}", outTradeNo, payStatus);
            return;
        }

        // 4. 非终态（NOTPAY 等）：推进 next_query_at（固定 3 秒，匹配调度频率）
        // 不再使用指数退避，用户明确要求"3s 一次"快速轮询
        LocalDateTime nextAt = LocalDateTime.now().plusSeconds(BASE_INTERVAL_SECONDS);
        payOrderService.setNextQueryTime(order.getId(), nextAt);
    }

    /**
     * 同步订单状态到本地 DB。
     */
    private void syncOrderStatus(PayOrder order, JsapiQueryResponse resp) {
        String payStatus = resp.getPayStatus();
        String targetStatus = payStatus2orderStatus(payStatus);
        if (targetStatus == null) {
            return;
        }

        // 幂等：仅状态未到终态时更新
        if (!OrderStatus.SUCCESS.equals(order.getStatus())
            && !OrderStatus.CLOSED.equals(order.getStatus())) {
            order.setStatus(targetStatus);
            if (PayStatus.SUCCESS.equals(payStatus) && resp.getSuccessTime() != null) {
                order.setSuccessTime(resp.getSuccessTime());
            }
            payOrderService.updateById(order);
            log.info("[轮询查单] ✅ 订单状态同步: outTradeNo={}, {} → {}",
                order.getOutTradeNo(), targetStatus, payStatus);
        }

        // 同步交易流水
        PayTransaction tx = payTransactionService.getByOutTradeNo(order.getOutTradeNo());
        if (tx != null) {
            tx.setPayStatus(payStatus);
            if (resp.getTransactionId() != null) tx.setTransactionId(resp.getTransactionId());
            if (resp.getAmountPayerTotal() != null) tx.setAmountPayerTotal(resp.getAmountPayerTotal());
            if (resp.getBankType() != null) tx.setBankType(resp.getBankType());
            if (PayStatus.SUCCESS.equals(payStatus) && resp.getSuccessTime() != null) {
                tx.setSuccessTime(resp.getSuccessTime());
            }
            payTransactionService.updateById(tx);
        }
    }

    /**
     * 微信支付状态 → 本地订单状态。
     */
    private String payStatus2orderStatus(String payStatus) {
        return switch (payStatus) {
            case PayStatus.SUCCESS -> OrderStatus.SUCCESS;
            case PayStatus.CLOSED -> OrderStatus.CLOSED;
            case PayStatus.REFUNDED -> OrderStatus.REFUNDED;
            default -> null;
        };
    }
}
