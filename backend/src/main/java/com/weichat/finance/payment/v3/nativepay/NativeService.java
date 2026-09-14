package com.weichat.finance.payment.v3.nativepay;

import com.weichat.finance.entity.MerchantConfig;

/**
 * Native 支付服务接口。
 *
 * <p>对应微信支付 V3：{@code POST /v3/pay/transactions/native}</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
public interface NativeService {

    /**
     * 创建 Native 支付订单，返回二维码链接 code_url。
     */
    NativeCreateResponse create(NativeCreateRequest request, MerchantConfig merchant);
}
