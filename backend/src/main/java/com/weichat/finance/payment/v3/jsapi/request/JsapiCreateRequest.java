package com.weichat.finance.payment.v3.jsapi.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * JSAPI 统一下单请求参数（业务层入参）。
 *
 * <p>对应微信支付 V3 接口：POST /v3/pay/transactions/jsapi</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
public class JsapiCreateRequest {

    /** 商户订单号（业务侧生成，UUID），必填，唯一。 */
    @NotBlank(message = "商户订单号不能为空")
    private String outTradeNo;

    /** 订单描述，必填，最长 127 字符。 */
    @NotBlank(message = "订单描述不能为空")
    private String description;

    /** 订单金额（分），必填。 */
    @NotNull(message = "订单金额不能为空")
    @Positive(message = "订单金额必须为正")
    private Long amountTotal;

    /** 货币类型，默认 CNY。 */
    private String currency = "CNY";

    /** 用户标识（JSAPI 必填），用户在商户 appid 下的唯一标识。 */
    @NotBlank(message = "JSAPI 必须传 openid")
    private String openid;

    /** 附加数据，原样回传，最长 128 字符。 */
    private String attach;

    /** 订单失效时间（可选）。 */
    private String timeExpire;

    public String getOutTradeNo() { return outTradeNo; }
    public void setOutTradeNo(String outTradeNo) { this.outTradeNo = outTradeNo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getAmountTotal() { return amountTotal; }
    public void setAmountTotal(Long amountTotal) { this.amountTotal = amountTotal; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }
    public String getAttach() { return attach; }
    public void setAttach(String attach) { this.attach = attach; }
    public String getTimeExpire() { return timeExpire; }
    public void setTimeExpire(String timeExpire) { this.timeExpire = timeExpire; }
}
