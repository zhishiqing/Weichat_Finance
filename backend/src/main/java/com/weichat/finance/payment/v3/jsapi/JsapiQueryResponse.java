package com.weichat.finance.payment.v3.jsapi;

import lombok.Data;

/**
 * 微信支付订单查询响应。
 *
 * <p>对应微信支付 V3 接口：GET /v3/pay/transactions/out-trade-no/{out_trade_no}</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
public class JsapiQueryResponse {

    /** 商户订单号 */
    private String outTradeNo;

    /** 微信支付订单号（SUCCESS 后才有） */
    private String transactionId;

    /** 支付状态：NOTPAY / SUCCESS / CLOSED / REVOKED / REFUNDED */
    private String payStatus;

    /** 用户实际支付金额（分） */
    private Long amountPayerTotal;

    /** 付款银行 */
    private String bankType;

    /** 支付成功时间（ISO 格式） */
    private String successTime;

    /** 数据来源：MOCK / REAL */
    private String source;
}
