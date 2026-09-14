package com.weichat.finance.entity.enums;

/**
 * 幂等操作类型。
 *
 * <p>对应 {@code t_pay_idempotent.operation} 字段。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
public final class IdempotentOperation {

    /** 创建订单（下单幂等） */
    public static final String CREATE_ORDER = "CREATE_ORDER";

    /** 退款（退款幂等） */
    public static final String REFUND = "REFUND";

    /** 回调处理（回调防重） */
    public static final String NOTIFY = "NOTIFY";

    private IdempotentOperation() {
        throw new AssertionError("枚举类禁止实例化");
    }
}
