package com.weichat.finance.entity.enums;

/**
 * 微信回调类型。
 *
 * <p>对应 {@code t_pay_notify_log.notify_type} 字段。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
public final class NotifyType {

    /** 支付成功通知 */
    public static final String PAY = "PAY";
    /** 退款结果通知 */
    public static final String REFUND = "REFUND";

    private NotifyType() {
        throw new AssertionError("枚举类禁止实例化");
    }
}
