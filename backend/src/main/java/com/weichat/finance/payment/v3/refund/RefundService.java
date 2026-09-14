package com.weichat.finance.payment.v3.refund;

import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.PayRefund;
import com.weichat.finance.payment.v3.refund.request.RefundCreateRequest;

/**
 * 退款服务接口（项目内部抽象）。
 *
 * <p>Phase 3 提供两套实现：</p>
 * <ul>
 *   <li>{@link MockRefundService}：返回假数据（默认）</li>
 *   <li>{@link RealRefundService}：调用微信支付 V3 真实接口（待 Phase 3.x）</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-14
 */
public interface RefundService {

    /**
     * 申请退款，返回微信退款单号。
     */
    RefundCreateResponse create(RefundCreateRequest request, MerchantConfig merchant, PayRefund refundEntity);

    /**
     * 查询退款单。
     */
    RefundCreateResponse query(String outRefundNo, MerchantConfig merchant);
}
