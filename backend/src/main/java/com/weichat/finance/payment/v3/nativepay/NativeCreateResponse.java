package com.weichat.finance.payment.v3.nativepay;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Native 支付下单响应。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@Schema(description = "Native 扫码支付下单响应")
public class NativeCreateResponse {

    @Schema(description = "二维码链接（用户扫码后即可支付）", example = "weixin://wxpay/bizpayurl?pr=XXXXX")
    private String codeUrl;

    @Schema(description = "数据来源（MOCK / REAL）", example = "MOCK", allowableValues = {"MOCK", "REAL"})
    private String source;
}
