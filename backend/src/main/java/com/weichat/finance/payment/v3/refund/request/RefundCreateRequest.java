package com.weichat.finance.payment.v3.refund.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 退款申请请求 DTO。
 *
 * <p>对应微信支付 V3：{@code POST /v3/refund/domestic/refunds}</p>
 *
 * <p>请求体字段采用 snake_case（与微信 API 一致），Jackson 默认支持 camelCase 到 snake_case 反序列化，
 * 通过 {@code spring.jackson.property-naming-strategy: SNAKE_CASE} 开启。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
public class RefundCreateRequest {

    /** 商户退款单号（必传，幂等键之一） */
    @NotBlank(message = "商户退款单号不能为空")
    @Size(max = 64)
    private String outRefundNo;

    /** 原商户订单号 */
    @NotBlank(message = "原商户订单号不能为空")
    @Size(max = 64)
    private String outTradeNo;

    /** 退款金额（分） */
    @NotNull(message = "退款金额不能为空")
    @Min(value = 1, message = "退款金额必须大于 0")
    private Long amountRefund;

    /** 原订单金额（分） */
    @NotNull(message = "原订单金额不能为空")
    @Min(value = 1)
    private Long amountTotal;

    /** 退款原因 */
    @Size(max = 255)
    private String reason;

    public String getOutRefundNo() { return outRefundNo; }
    public void setOutRefundNo(String outRefundNo) { this.outRefundNo = outRefundNo; }
    public String getOutTradeNo() { return outTradeNo; }
    public void setOutTradeNo(String outTradeNo) { this.outTradeNo = outTradeNo; }
    public Long getAmountRefund() { return amountRefund; }
    public void setAmountRefund(Long amountRefund) { this.amountRefund = amountRefund; }
    public Long getAmountTotal() { return amountTotal; }
    public void setAmountTotal(Long amountTotal) { this.amountTotal = amountTotal; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
