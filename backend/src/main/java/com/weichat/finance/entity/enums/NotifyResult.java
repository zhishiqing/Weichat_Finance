package com.weichat.finance.entity.enums;

/**
 * 回调处理结果。
 *
 * <p>对应 {@code t_pay_notify_log.verify_result} 和 {@code t_pay_notify_log.process_result} 字段。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
public final class NotifyResult {

    /** 签名校验通过 */
    public static final String PASS = "PASS";

    /** 签名校验失败 */
    public static final String FAIL = "FAIL";

    /** 跳过验签（Phase 2 占位，待真实私钥补齐后激活验签流程） */
    public static final String SKIPPED_PHASE3 = "SKIPPED_PHASE3";

    /** 业务处理成功 */
    public static final String SUCCESS = "SUCCESS";

    /** 业务处理失败 */
    public static final String FAILED = "FAILED";

    /** 业务被忽略（如 SIGNTEST 探测流量或重复通知） */
    public static final String IGNORED = "IGNORED";

    /** 业务被忽略（Phase 2 占位，待真实私钥激活验签/解密） */
    public static final String IGNORED_PHASE3 = "IGNORED_PHASE3";

    private NotifyResult() {
        throw new AssertionError("枚举类禁止实例化");
    }
}
