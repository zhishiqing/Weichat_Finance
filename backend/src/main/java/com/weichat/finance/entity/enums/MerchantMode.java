package com.weichat.finance.entity.enums;

/**
 * 商户模式。
 *
 * <p>对应 {@code t_merchant_config.mode} 字段。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
public final class MerchantMode {

    /** 直连商户（v1.0） */
    public static final String DIRECT = "DIRECT";

    /** 服务商（v2.0） */
    public static final String PARTNER = "PARTNER";

    private MerchantMode() {
        throw new AssertionError("枚举类禁止实例化");
    }
}
