package com.weichat.finance.payment.v3.refund;

import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.PayRefund;
import com.weichat.finance.payment.v3.refund.request.RefundCreateRequest;

/**
 * 真实退款服务（生产模式）· Phase 3 占位。
 *
 * @author panhw
 * @since 2026-09-14
 */
public class RealRefundService implements RefundService {

    @Override
    public RefundCreateResponse create(RefundCreateRequest request, MerchantConfig merchant, PayRefund refundEntity) {
        throw new UnsupportedOperationException(
            "RealRefundService Phase 3 占位实现，请将 wechatpay.mode 切换为 MOCK 验证流程，"
            + "或等待 Phase 3.x 实施完成后激活");
    }

    @Override
    public RefundCreateResponse query(String outRefundNo, MerchantConfig merchant) {
        throw new UnsupportedOperationException(
            "RealRefundService Phase 3 占位实现，请将 wechatpay.mode 切换为 MOCK 验证流程，"
            + "或等待 Phase 3.x 实施完成后激活");
    }
}
