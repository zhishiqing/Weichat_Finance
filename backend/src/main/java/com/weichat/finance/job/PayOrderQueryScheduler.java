package com.weichat.finance.job;

import com.weichat.finance.entity.PayOrder;
import com.weichat.finance.entity.PayOrderQueryLog;
import com.weichat.finance.entity.enums.OrderStatus;
import com.weichat.finance.entity.enums.PayStatus;
import com.weichat.finance.payment.config.WechatPayProperties;
import com.weichat.finance.payment.v3.jsapi.JsapiQueryResponse;
import com.weichat.finance.payment.v3.jsapi.JsapiService;
import com.weichat.finance.service.PayOrderQueryLogService;
import com.weichat.finance.service.PayOrderService;
import com.weichat.finance.service.PayTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 定时查单兜底任务（每 30 分钟执行一次）。
 *
 * <p>主动向微信侧查询未支付订单的真实状态，解决回调失败/丢失导致的订单永久悬挂问题。</p>
 *
 * <h3>执行流程</h3>
 * <pre>
 * 1. 扫描 t_pay_order 中 status IN (CREATED, SUBMITTING) 且 last_query_time 已过期（或为空）的订单
 * 2. 对每笔订单：
 *    a. 调用 JsapiService.queryByOutTradeNo() 查微信侧状态
 *    b. 状态变化时同步更新 t_pay_order + t_pay_transaction
 *    c. 更新 last_query_time 防止本批次重复查
 * 3. 记录执行日志 t_pay_order_query_log
 * </pre>
 *
 * <h3>Mock 模式行为</h3>
 * 仅打日志，不调用微信接口，不更新数据库。
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>分批处理（每批 100 条），避免长时间锁表</li>
 *   <li>幂等：last_query_time 防重，幂等键兜底</li>
 *   <li>指数退避：查单失败重试 3 次（1s → 2s → 4s）</li>
 *   <li>不处理已关闭订单（CLOSED 状态不再查）</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Component
public class PayOrderQueryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PayOrderQueryScheduler.class);

    /** 每批最大处理条数 */
    private static final int BATCH_SIZE = 100;

    /** 下单后等待 N 分钟再查（避免和下单并发冲突，通常下单到回调 1-3 分钟） */
    private static final int MINUTES_AFTER_CREATE = 10;

    /** 查单失败重试次数 */
    private static final int MAX_RETRIES = 3;

    @Autowired
    private PayOrderService payOrderService;

    @Autowired
    private PayTransactionService payTransactionService;

    @Autowired
    private PayOrderQueryLogService payOrderQueryLogService;

    @Autowired
    private WechatPayProperties wechatPayProperties;

    /** JSAPI 查单服务（MOCK / REAL 由配置决定） */
    @Autowired(required = false)
    private JsapiService jsapiService;

    /**
     * 每 30 分钟执行一次（cron 从 application.yml 读取，支持动态调整）。
     * 首次启动后等待 1 分钟再执行第一次，之后按 cron 周期执行。
     *
     * <p>固定 30 分钟的原因：微信订单有效期 2 小时，30 分钟兜底足够及时发现问题，
     * 且不会对微信接口造成过大压力。</p>
     */
    @Scheduled(fixedRateString = "${scheduler.pay-order-query.interval-ms:1800000}",
               initialDelayString = "${scheduler.pay-order-query.initial-delay-ms:60000}")
    public void queryPendingOrders() {
        LocalDateTime now = LocalDateTime.now();
        String batchNo = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + "_" + String.format("%04d", (int) (Math.random() * 10000));
        LocalDateTime scanStart = now.minusMinutes(MINUTES_AFTER_CREATE);

        log.info("[查单兜底] 批次 {} 开始扫描，扫描时间窗口: {}", batchNo, scanStart);

        // MOCK 模式：只打日志，不实际查
        if ("MOCK".equalsIgnoreCase(wechatPayProperties.getMode())) {
            handleMockMode(batchNo, scanStart);
            return;
        }

        // REAL 模式：执行真正查单逻辑
        handleRealMode(batchNo, scanStart);
    }

    /**
     * Mock 模式：扫描但不实际查单，用于本地验证调度是否正常工作。
     */
    private void handleMockMode(String batchNo, LocalDateTime scanStart) {
        List<PayOrder> hangingOrders = payOrderService.listHangingOrders(scanStart, BATCH_SIZE);
        log.info("[MOCK] 批次 {} 扫描到 {} 笔悬挂单（MOCK 模式，不查微信）", batchNo, hangingOrders.size());

        PayOrderQueryLog queryLog = new PayOrderQueryLog();
        queryLog.setBatchNo(batchNo);
        queryLog.setScanStartTime(LocalDateTime.now());
        queryLog.setScanOrderCount(hangingOrders.size());
        queryLog.setQueryOrderCount(0);
        queryLog.setSuccessCount(0);
        queryLog.setFailCount(0);
        queryLog.setNotpayCount(0);
        queryLog.setClosedCount(0);
        queryLog.setUpdatedCount(0);
        queryLog.setCostMs(0L);
        queryLog.setErrorMessage("MOCK 模式，不查微信");
        payOrderQueryLogService.save(queryLog);
    }

    /**
     * REAL 模式：真正查微信并同步状态。
     */
    private void handleRealMode(String batchNo, LocalDateTime scanStart) {
        LocalDateTime batchStart = LocalDateTime.now();
        AtomicInteger scanCount = new AtomicInteger(0);
        AtomicInteger queryCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger notpayCount = new AtomicInteger(0);
        AtomicInteger closedCount = new AtomicInteger(0);
        AtomicInteger updatedCount = new AtomicInteger(0);
        AtomicReference<String> worstError = new AtomicReference<>(null);

        while (true) {
            List<PayOrder> orders = payOrderService.listHangingOrders(scanStart, BATCH_SIZE);
            if (orders.isEmpty()) {
                break;
            }
            scanCount.addAndGet(orders.size());

            for (PayOrder order : orders) {
                processOrder(order, batchNo, scanStart,
                    queryCount, successCount, failCount,
                    notpayCount, closedCount, updatedCount, worstError);
            }
        }

        LocalDateTime batchEnd = LocalDateTime.now();
        long costMs = java.time.Duration.between(batchStart, batchEnd).toMillis();

        // 记录执行日志
        PayOrderQueryLog queryLog = new PayOrderQueryLog();
        queryLog.setBatchNo(batchNo);
        queryLog.setScanStartTime(batchStart);
        queryLog.setScanEndTime(batchEnd);
        queryLog.setScanOrderCount(scanCount.get());
        queryLog.setQueryOrderCount(queryCount.get());
        queryLog.setSuccessCount(successCount.get());
        queryLog.setFailCount(failCount.get());
        queryLog.setNotpayCount(notpayCount.get());
        queryLog.setClosedCount(closedCount.get());
        queryLog.setUpdatedCount(updatedCount.get());
        queryLog.setCostMs(costMs);
        queryLog.setErrorMessage(worstError.get());
        payOrderQueryLogService.save(queryLog);

        log.info("[查单兜底] 批次 {} 完成，扫描={} 查询={} 成功={} 失败={} " +
                 "未变化={} 关闭={} 状态变化={} 耗时={}ms",
            batchNo, scanCount.get(), queryCount.get(), successCount.get(),
            failCount.get(), notpayCount.get(), closedCount.get(), updatedCount.get(), costMs);
    }

    /**
     * 处理单笔订单：查单 → 同步状态 → 更新 last_query_time。
     */
    private void processOrder(PayOrder order, String batchNo, LocalDateTime scanStart,
                              AtomicInteger queryCount, AtomicInteger successCount,
                              AtomicInteger failCount, AtomicInteger notpayCount,
                              AtomicInteger closedCount, AtomicInteger updatedCount,
                              AtomicReference<String> worstError) {
        String outTradeNo = order.getOutTradeNo();
        log.debug("[查单兜底] 批次 {} 正在查: outTradeNo={}, 当前状态={}",
            batchNo, outTradeNo, order.getStatus());

        JsapiQueryResponse resp = null;
        Exception lastException = null;

        // 指数退避重试
        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            try {
                resp = jsapiService.queryByOutTradeNo(outTradeNo,
                    new com.weichat.finance.entity.MerchantConfig());
                break; // 成功，跳出重试循环
            } catch (Exception e) {
                lastException = e;
                log.warn("[查单兜底] 批次 {} 查询 outTradeNo={} 失败，重试 {}/{}: {}",
                    batchNo, outTradeNo, retry + 1, MAX_RETRIES, e.getMessage());
                if (retry < MAX_RETRIES - 1) {
                    try {
                        // 指数退避：1s → 2s → 4s
                        Thread.sleep((long) Math.pow(2, retry) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        queryCount.incrementAndGet();

        if (resp == null) {
            failCount.incrementAndGet();
            String msg = lastException != null ? lastException.getMessage() : "未知错误";
            if (worstError.get() == null) {
                worstError.set("outTradeNo=" + outTradeNo + ": " + msg);
            }
            log.error("[查单兜底] 批次 {} 查询 outTradeNo={} 最终失败: {}", batchNo, outTradeNo, msg);
            updateLastQueryTime(order.getId());
            return;
        }

        successCount.incrementAndGet();
        String payStatus = resp.getPayStatus();
        log.info("[查单兜底] 批次 {} 查询 outTradeNo={} 结果: payStatus={}", batchNo, outTradeNo, payStatus);

        // 同步状态
        boolean changed = syncOrderStatus(order, resp);

        if (changed) {
            updatedCount.incrementAndGet();
            log.info("[查单兜底] ✅ 批次 {} 状态同步生效: outTradeNo={}，{} → {}",
                batchNo, outTradeNo, order.getStatus(),
                changed ? payStatus2orderStatus(payStatus) : "(未变化)");
        }

        // 统计各状态
        switch (payStatus) {
            case PayStatus.NOTPAY -> notpayCount.incrementAndGet();
            case PayStatus.CLOSED -> closedCount.incrementAndGet();
            case PayStatus.SUCCESS, PayStatus.REFUNDED -> { /* updatedCount 已计入 */ }
            default -> { /* REVOKED 等，暂不处理 */ }
        }

        // 更新 last_query_time（无论状态是否变化）
        updateLastQueryTime(order.getId());
    }

    /**
     * 同步订单状态到本地 DB。
     *
     * @return 是否发生了状态变化（兜底生效）
     */
    private boolean syncOrderStatus(PayOrder order, JsapiQueryResponse resp) {
        String payStatus = resp.getPayStatus();
        String targetOrderStatus = payStatus2orderStatus(payStatus);

        if (targetOrderStatus == null) {
            return false;
        }

        // 状态未变化，跳过
        if (targetOrderStatus.equals(order.getStatus())) {
            return false;
        }

        // 更新 t_pay_order
        order.setStatus(targetOrderStatus);
        if (PayStatus.SUCCESS.equals(payStatus) && resp.getSuccessTime() != null) {
            order.setSuccessTime(resp.getSuccessTime());
        }
        payOrderService.updateById(order);

        // 更新 t_pay_transaction
        if (PayStatus.SUCCESS.equals(payStatus)) {
            payTransactionService.updateByOutTradeNo(
                order.getOutTradeNo(),
                payStatus,
                resp.getTransactionId(),
                resp.getAmountPayerTotal(),
                resp.getBankType(),
                resp.getSuccessTime()
            );
        } else {
            payTransactionService.updateByOutTradeNo(
                order.getOutTradeNo(), payStatus, null, null, null, null);
        }

        return true;
    }

    /**
     * 微信支付状态 → 本地订单状态。
     */
    private String payStatus2orderStatus(String payStatus) {
        return switch (payStatus) {
            case PayStatus.SUCCESS -> OrderStatus.SUCCESS;
            case PayStatus.CLOSED -> OrderStatus.CLOSED;
            case PayStatus.NOTPAY, PayStatus.REVOKED, PayStatus.REFUNDED -> null; // 不变化
            default -> null;
        };
    }

    /**
     * 更新订单的 last_query_time 字段。
     */
    private void updateLastQueryTime(Long orderId) {
        try {
            PayOrder update = new PayOrder();
            update.setId(orderId);
            update.setLastQueryTime(LocalDateTime.now());
            payOrderService.updateById(update);
        } catch (Exception e) {
            log.warn("[查单兜底] 更新 last_query_time 失败: orderId={}, {}", orderId, e.getMessage());
        }
    }
}
