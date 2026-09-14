package com.weichat.finance.payment.v3.jsapi;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * JSAPI 查询订单响应。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@Schema(description = "JSAPI 查询订单响应")
public class JsapiQueryResponse {

    @Schema(description = "商户订单号", example = "ORDER_20260914_001")
    private String outTradeNo;

    @Schema(description = "微信支付订单号", example = "4200001234202309156060123456789")
    private String transactionId;

    @Schema(description = "支付状态", example = "SUCCESS",
        allowableValues = {"NOTPAY", "SUCCESS", "CLOSED", "REVOKED", "REFUNDED"})
    private String payStatus;

    @Schema(description = "用户实际支付金额（分）", example = "100")
    private Long amountPayerTotal;

    @Schema(description = "付款银行类型", example = "CMC")
    private String bankType;

    @Schema(description = "支付成功时间")
    private LocalDateTime successTime;

    @Schema(description = "数据来源（MOCK / REAL）", example = "MOCK", allowableValues = {"MOCK", "REAL"})
    private String source;
}
