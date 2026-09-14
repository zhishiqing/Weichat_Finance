package com.weichat.finance.entity.enums;

/**
 * 幂等操作结果。
 *
 * <p>对应 {@code t_pay_idempotent.result_code} 字段。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
public final class IdempotentResult {

    /** 处理中（业务未完成） */
    public static final String PROCESSING = "PROCESSING";

    /** 成功 */
    public static final String SUCCESS = "SUCCESS";

    /** 失败 */
    public static final String FAILED = "FAILED";

    private IdempotentResult() {
        throw new AssertionError("枚举类禁止实例化");
    }
}
