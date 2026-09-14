package com.weichat.finance.payment.v3.refund.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

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
@Data
@Schema(description = "退款申请请求")
public class RefundCreateRequest {

    @Schema(description = "商户退款单号（业务侧生成，唯一，幂等键）",
        example = "REFUND_20260914_001",
        requiredMode = Schema.RequiredMode.REQUIRED,
        maxLength = 64)
    @NotBlank(message = "商户退款单号不能为空")
    @Size(max = 64)
    private String outRefundNo;

    @Schema(description = "原商户订单号（关联 t_pay_order.out_trade_no）",
        example = "ORDER_20260914_001",
        requiredMode = Schema.RequiredMode.REQUIRED,
        maxLength = 64)
    @NotBlank(message = "原商户订单号不能为空")
    @Size(max = 64)
    private String outTradeNo;

    @Schema(description = "退款金额（单位：分，必须为正整数，且不超过原订单金额）",
        example = "30",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "退款金额不能为空")
    @Min(value = 1, message = "退款金额必须大于 0")
    private Long amountRefund;

    @Schema(description = "原订单金额（单位：分，便于服务端校验退款金额上限）",
        example = "100",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "原订单金额不能为空")
    @Min(value = 1)
    private Long amountTotal;

    @Schema(description = "退款原因", example = "用户主动申请退款", maxLength = 255)
    @Size(max = 255)
    private String reason;
}
