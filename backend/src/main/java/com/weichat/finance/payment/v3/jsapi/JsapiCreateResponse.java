package com.weichat.finance.payment.v3.jsapi;

/**
 * JSAPI 统一下单响应。
 *
 * <p>前端拿到 prepay_id 后，用 WeixinJSBridge 调起支付。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
public class JsapiCreateResponse {

    /** 预支付交易会话标识（前端的 package 字段 = "prepay_id=" + 此值）。 */
    private String prepayId;

    /** 数据来源：MOCK / REAL，便于排查。 */
    private String source;

    public String getPrepayId() { return prepayId; }
    public void setPrepayId(String prepayId) { this.prepayId = prepayId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
