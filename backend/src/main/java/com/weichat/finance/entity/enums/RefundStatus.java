package com.weichat.finance.entity.enums;

/**
 * 退款单状态。
 *
 * <p>对应 {@code t_pay_refund.refund_status} 字段。</p>
 *
 * <pre>
 * 状态机：
 *
 *   ┌────────────┐  受理成功  ┌─────────┐  到账完成 ┌─────────┐
 *   │ (initial)  ├──────────▶│PROCESSING├─────────▶│ SUCCESS │
 *   └────────────┘           └─────────┘          └─────────┘
 *                                  │
 *                                  │ 异常
 *                                  ▼
 *                          ┌─────────┐  超时撤销 ┌─────────┐
 *                          │ABNORMAL ├──────────▶│ CLOSED  │
 *                          └─────────┘           └─────────┘
 * </pre>
 *
 * @author panhw
 * @since 2026-09-14
 */
public final class RefundStatus {

    /** 退款中（已受理，等待资金到账） */
    public static final String PROCESSING = "PROCESSING";
    /** 退款成功 */
    public static final String SUCCESS = "SUCCESS";
    /** 退款关闭（异常后被撤销） */
    public static final String CLOSED = "CLOSED";
    /** 退款异常（银行侧失败等原因） */
    public static final String ABNORMAL = "ABNORMAL";

    private RefundStatus() {
        throw new AssertionError("枚举类禁止实例化");
    }
}
