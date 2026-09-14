package com.weichat.finance.entity.enums;

/**
 * 业务订单状态。
 *
 * <p>对应 {@code t_pay_order.status} 字段。</p>
 *
 * <pre>
 * 状态机（简化）：
 *
 *   ┌──────────┐  create  ┌─────────────┐
 *   │ (none)   ├─────────▶│  SUBMITTING │
 *   └──────────┘          └─────────────┘
 *                              │
 *                              │ 服务端受理成功
 *                              ▼
 *                         ┌─────────┐  close   ┌─────────┐
 *                         │ CREATED ├─────────▶│ CLOSED  │
 *                         └─────────┘          └─────────┘
 *                              │
 *                              │ 用户支付成功（回调）
 *                              ▼
 *                         ┌─────────┐  refund  ┌────────────┐  refund_done  ┌──────────┐
 *                         │ SUCCESS ├─────────▶│ REFUNDING  ├──────────────▶│ REFUNDED │
 *                         └─────────┘          └────────────┘               └──────────┘
 * </pre>
 *
 * @author panhw
 * @since 2026-09-14
 */
public final class OrderStatus {

    /** 受理中（已落库，待服务端返回 prepay_id） */
    public static final String SUBMITTING = "SUBMITTING";
    /** 已创建（微信侧已受理，可调起支付） */
    public static final String CREATED = "CREATED";
    /** 支付成功 */
    public static final String SUCCESS = "SUCCESS";
    /** 已关闭（关单后） */
    public static final String CLOSED = "CLOSED";
    /** 退款中 */
    public static final String REFUNDING = "REFUNDING";
    /** 已退款 */
    public static final String REFUNDED = "REFUNDED";

    private OrderStatus() {
        throw new AssertionError("枚举类禁止实例化");
    }
}
