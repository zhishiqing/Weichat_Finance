package com.weichat.finance.payment.v3.nativepay;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Native 支付下单请求。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@Schema(description = "Native 扫码支付下单请求")
public class NativeCreateRequest {

    @Schema(description = "商户订单号", example = "ORDER_20260914_002", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 32)
    @NotBlank(message = "商户订单号不能为空")
    @Size(max = 32)
    private String outTradeNo;

    @Schema(description = "订单描述", example = "扫码商品", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 127)
    @NotBlank(message = "订单描述不能为空")
    @Size(max = 127)
    private String description;

    @Schema(description = "订单金额（单位：分，必须为正整数）", example = "200", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单金额不能为空")
    @Positive(message = "订单金额必须为正")
    private Long amountTotal;

    @Schema(description = "货币类型（默认 CNY）", example = "CNY", defaultValue = "CNY")
    private String currency;

    @Schema(description = "附加数据（原样回传）", example = "{\"k\":\"v\"}", maxLength = 128)
    @Size(max = 128)
    private String attach;
}
