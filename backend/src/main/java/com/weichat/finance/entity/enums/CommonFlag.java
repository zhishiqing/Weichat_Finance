package com.weichat.finance.entity.enums;

/**
 * 全局通用枚举：启用/禁用、逻辑删除。
 *
 * @author panhw
 * @since 2026-09-14
 */
public final class CommonFlag {

    /** 禁用 */
    public static final int DISABLED = 0;

    /** 启用 */
    public static final int ENABLED = 1;

    /** 未删除 */
    public static final int NOT_DELETED = 0;

    /** 已删除（逻辑删除） */
    public static final int DELETED = 1;

    private CommonFlag() {
        throw new AssertionError("枚举类禁止实例化");
    }
}
