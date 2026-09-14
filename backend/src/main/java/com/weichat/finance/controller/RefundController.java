package com.weichat.finance.controller;

import com.weichat.finance.common.R;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.PayOrder;
import com.weichat.finance.entity.PayRefund;
import com.weichat.finance.payment.v3.refund.RefundCreateResponse;
import com.weichat.finance.payment.v3.refund.RefundService;
import com.weichat.finance.payment.v3.refund.request.RefundCreateRequest;
import com.weichat.finance.service.MerchantConfigService;
import com.weichat.finance.service.PayOrderService;
import com.weichat.finance.service.PayRefundService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 退款相关接口。
 *
 * <p>对外暴露：</p>
 * <ul>
 *   <li>{@code POST /api/v1/payment/refund/create} · 申请退款</li>
 *   <li>{@code GET  /api/v1/payment/refund/{outRefundNo}} · 查询退款单</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-14
 */
@RestController
@RequestMapping("/v1/payment")
public class RefundController {

    private static final Logger log = LoggerFactory.getLogger(RefundController.class);
    private static final String DEFAULT_MCH_ID = "PLACEHOLDER_MCH_ID";

    private final RefundService refundService;
    private final MerchantConfigService merchantConfigService;
    private final PayOrderService payOrderService;
    private final PayRefundService payRefundService;

    public RefundController(RefundService refundService,
                             MerchantConfigService merchantConfigService,
                             PayOrderService payOrderService,
                             PayRefundService payRefundService) {
        this.refundService = refundService;
        this.merchantConfigService = merchantConfigService;
        this.payOrderService = payOrderService;
        this.payRefundService = payRefundService;
    }

    /**
     * 申请退款（幂等：按 outRefundNo 判重）。
     */
    @PostMapping("/refund/create")
    public R<RefundCreateResponse> createRefund(@Valid @RequestBody RefundCreateRequest request) {
        log.info("申请退款: outRefundNo={}, outTradeNo={}, amountRefund={}分",
            request.getOutRefundNo(), request.getOutTradeNo(), request.getAmountRefund());

        PayRefund existing = payRefundService.getByOutRefundNo(request.getOutRefundNo());
        if (existing != null) {
            log.warn("商户退款单号已存在: outRefundNo={}", request.getOutRefundNo());
            RefundCreateResponse resp = new RefundCreateResponse();
            resp.setOutRefundNo(existing.getOutRefundNo());
            resp.setRefundId(existing.getRefundId());
            resp.setRefundStatus(existing.getRefundStatus());
            resp.setSource("REPLAY");
            return R.ok(resp);
        }

        PayOrder order = payOrderService.getByOutTradeNo(request.getOutTradeNo());
        if (order == null) {
            return R.fail(404, "原商户订单号不存在");
        }

        // 🔴 致命修复：金额字段以后端为准。前端传值仅作引导。
        // 覆盖前端传入的 amountRefund/amountTotal，防止恶意覆盖
        request.setAmountTotal(order.getAmountTotal());

        // 🔴 致命修复：退款金额不能超过原订单金额
        if (request.getAmountRefund() > order.getAmountTotal()) {
            log.warn("退款金额超出原订单金额: amountRefund={}, amountTotal={}, outTradeNo={}",
                request.getAmountRefund(), order.getAmountTotal(), request.getOutTradeNo());
            return R.fail(400, "退款金额不能超过原订单金额");
        }

        // 计算已退金额，防止重复超额退款
        Long alreadyRefunded = payRefundService.lambdaQuery()
            .eq(PayRefund::getOutTradeNo, request.getOutTradeNo())
            .in(PayRefund::getRefundStatus, "PROCESSING", "SUCCESS")
            .list()
            .stream()
            .mapToLong(r -> r.getAmountRefund() == null ? 0L : r.getAmountRefund())
            .sum();
        if (alreadyRefunded + request.getAmountRefund() > order.getAmountTotal()) {
            log.warn("累计退款金额将超出原订单金额: alreadyRefunded={}, currentRequest={}, orderTotal={}",
                alreadyRefunded, request.getAmountRefund(), order.getAmountTotal());
            return R.fail(400, "累计退款金额已超限，剩余可退金额=" + (order.getAmountTotal() - alreadyRefunded) + "分");
        }

        MerchantConfig merchant = merchantConfigService.getByMchId(order.getMchId() == null
            ? DEFAULT_MCH_ID : order.getMchId());
        if (merchant == null) {
            return R.fail(500, "商户配置缺失");
        }

        // 落退款单
        PayRefund refundEntity = new PayRefund();
        refundEntity.setOutRefundNo(request.getOutRefundNo());
        refundEntity.setOutTradeNo(request.getOutTradeNo());
        refundEntity.setMchId(merchant.getMchId());
        refundEntity.setAmountRefund(request.getAmountRefund());
        refundEntity.setAmountTotal(request.getAmountTotal());
        refundEntity.setReason(request.getReason());
        refundEntity.setRefundStatus("PROCESSING");
        refundEntity.setNotifyUrl(merchant.getNotifyUrlBase() + "/notify/v3/refund/success");
        payRefundService.save(refundEntity);

        // 调用服务（Mock / Real）
        RefundCreateResponse response = refundService.create(request, merchant, refundEntity);

        // 回填微信侧信息
        refundEntity.setRefundId(response.getRefundId());
        refundEntity.setRefundStatus(response.getRefundStatus());
        payRefundService.updateById(refundEntity);

        // 业务订单状态
        order.setStatus("REFUNDING");
        payOrderService.updateById(order);

        return R.ok(response);
    }

    /**
     * 查询退款单。
     */
    @GetMapping("/refund/{outRefundNo}")
    public R<RefundCreateResponse> queryRefund(@PathVariable String outRefundNo) {
        log.info("查询退款: outRefundNo={}", outRefundNo);

        PayRefund refund = payRefundService.getByOutRefundNo(outRefundNo);
        if (refund == null) {
            return R.fail(404, "商户退款单号不存在");
        }

        MerchantConfig merchant = merchantConfigService.getByMchId(refund.getMchId());
        if (merchant == null) {
            return R.fail(500, "商户配置缺失");
        }

        return R.ok(refundService.query(outRefundNo, merchant));
    }
}
