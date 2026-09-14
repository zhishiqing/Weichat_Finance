package com.weichat.finance.payment.v3.nativepay;

/**
 * Native 支付下单响应。
 *
 * @author panhw
 * @since 2026-09-14
 */
public class NativeCreateResponse {

    /** 二维码链接（用户扫码后即可支付） */
    private String codeUrl;

    /** 数据来源：MOCK / REAL */
    private String source;

    public String getCodeUrl() { return codeUrl; }
    public void setCodeUrl(String codeUrl) { this.codeUrl = codeUrl; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
