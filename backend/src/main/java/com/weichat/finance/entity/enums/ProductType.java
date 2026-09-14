package com.weichat.finance.entity.enums;

/**
 * 支付产品类型。
 *
 * <p>对应 {@code t_pay_order.product_type} 字段。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
public final class ProductType {

    /** 公众号 / 小程序内支付（用户 openid 必填） */
    public static final String JSAPI = "JSAPI";

    /** 扫码支付（PC 网站 / 线下二维码） */
    public static final String NATIVE = "NATIVE";

    private ProductType() {
        throw new AssertionError("枚举类禁止实例化");
    }
}
