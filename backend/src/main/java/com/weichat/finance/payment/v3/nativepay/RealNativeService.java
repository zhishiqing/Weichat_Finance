package com.weichat.finance.payment.v3.nativepay;

import com.weichat.finance.entity.MerchantConfig;

/**
 * 真实 Native 支付服务 · Phase 2.2 占位。
 *
 * @author panhw
 * @since 2026-09-14
 */
public class RealNativeService implements NativeService {

    @Override
    public NativeCreateResponse create(NativeCreateRequest request, MerchantConfig merchant) {
        throw new UnsupportedOperationException(
            "RealNativeService Phase 2.2 占位实现，请将 wechatpay.mode 切换为 MOCK 验证流程，"
            + "或等待 Phase 2.x 实施完成后激活");
    }
}
