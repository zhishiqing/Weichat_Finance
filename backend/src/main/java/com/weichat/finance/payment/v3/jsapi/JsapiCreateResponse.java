package com.weichat.finance.payment.v3.jsapi;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * JSAPI 统一下单响应（调起支付用）。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@Schema(description = "JSAPI 统一下单响应")
public class JsapiCreateResponse {

    @Schema(description = "预支付会话标识", example = "wx2014102720093954e6e7d1a01234567")
    private String prepayId;

    @Schema(description = "数据来源（MOCK / REAL）", example = "MOCK", allowableValues = {"MOCK", "REAL"})
    private String source;
}
