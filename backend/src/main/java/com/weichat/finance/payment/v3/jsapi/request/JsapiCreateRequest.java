package com.weichat.finance.payment.v3.jsapi.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * JSAPI 统一下单请求参数（业务层入参）。
 *
 * <p>对应微信支付 V3 接口：POST /v3/pay/transactions/jsapi</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@Schema(description = "JSAPI 统一下单请求参数")
public class JsapiCreateRequest {

    @Schema(description = "商户订单号", example = "ORDER_20260914_001", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 32)
    @NotBlank(message = "商户订单号不能为空")
    @Size(max = 32, message = "商户订单号最长 32 字符")
    private String outTradeNo;

    @Schema(description = "订单描述", example = "商品名称", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 127)
    @NotBlank(message = "订单描述不能为空")
    @Size(max = 127, message = "订单描述最长 127 字符")
    private String description;

    @Schema(description = "订单金额（单位：分，必须为正整数）", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单金额不能为空")
    @Positive(message = "订单金额必须为正")
    private Long amountTotal;

    @Schema(description = "货币类型（默认 CNY）", example = "CNY", defaultValue = "CNY", maxLength = 16)
    private String currency = "CNY";

    @Schema(description = "用户标识（JSAPI 必填）", example = "oUpF8uMuAJxxyfBWk2e3tR3R6_T4", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 128)
    @NotBlank(message = "JSAPI 必须传 openid")
    @Size(max = 128, message = "openid 最长 128 字符")
    private String openid;

    @Schema(description = "附加数据（原样回传）", example = "{\"k\":\"v\"}", maxLength = 128)
    @Size(max = 128, message = "attach 最长 128 字符")
    private String attach;

    @Schema(description = "订单失效时间（ISO-8601 UTC，如 2026-12-31T23:59:59+08:00）", example = "2026-12-31T23:59:59+08:00")
    private String timeExpire;
}
