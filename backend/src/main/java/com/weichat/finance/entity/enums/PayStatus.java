package com.weichat.finance.entity.enums;

/**
 * 微信侧支付状态。
 *
 * <p>对应 {@code t_pay_transaction.pay_status} 字段。</p>
 *
 * <p>来自微信支付 V3 接口 {@code GET /v3/pay/transactions/out-trade-no/{out_trade_no}} 返回。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
public final class PayStatus {

    /** 未支付（订单已创建，等待用户支付） */
    public static final String NOTPAY = "NOTPAY";
    /** 支付成功 */
    public static final String SUCCESS = "SUCCESS";
    /** 已关闭（关单或超时） */
    public static final String CLOSED = "CLOSED";
    /** 已撤销（付款码支付专用） */
    public static final String REVOKED = "REVOKED";
    /** 已全额退款 */
    public static final String REFUNDED = "REFUNDED";

    private PayStatus() {
        throw new AssertionError("枚举类禁止实例化");
    }
}
