package com.weichat.finance.payment.v3.nativepay;

/**
 * Native 支付下单请求。
 *
 * @author panhw
 * @since 2026-09-14
 */
public class NativeCreateRequest {

    private String outTradeNo;
    private String description;
    private Long amountTotal;
    private String currency;
    private String attach;

    public String getOutTradeNo() { return outTradeNo; }
    public void setOutTradeNo(String outTradeNo) { this.outTradeNo = outTradeNo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getAmountTotal() { return amountTotal; }
    public void setAmountTotal(Long amountTotal) { this.amountTotal = amountTotal; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getAttach() { return attach; }
    public void setAttach(String attach) { this.attach = attach; }
}
