package com.weichat.finance.payment.v3.refund;

import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.PayRefund;
import com.weichat.finance.entity.enums.RefundStatus;
import com.weichat.finance.payment.v3.refund.request.RefundCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Mock 退款服务（默认）。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Service
@ConditionalOnProperty(prefix = "wechatpay", name = "mode", havingValue = "MOCK", matchIfMissing = true)
public class MockRefundService implements RefundService {

    private static final Logger log = LoggerFactory.getLogger(MockRefundService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "abcdef0123456789";

    @Override
    public RefundCreateResponse create(RefundCreateRequest request, MerchantConfig merchant, PayRefund refundEntity) {
        log.warn("当前使用 MockRefundService，退款不会到达微信支付服务端，仅用于本地调试。");
        log.info("[MOCK] 申请退款: outRefundNo={}, outTradeNo={}, amountRefund={}分",
            request.getOutRefundNo(), request.getOutTradeNo(), request.getAmountRefund());

        RefundCreateResponse response = new RefundCreateResponse();
        response.setOutRefundNo(request.getOutRefundNo());
        response.setRefundId("MOCK_refund_" + randomHex(20));
        response.setRefundStatus(RefundStatus.PROCESSING);
        response.setCreatedAt(LocalDateTime.now());
        response.setSource("MOCK");
        log.info("[MOCK] 退款受理成功: refundId={}", response.getRefundId());
        return response;
    }

    @Override
    public RefundCreateResponse query(String outRefundNo, MerchantConfig merchant) {
        log.info("[MOCK] 查询退款: outRefundNo={}", outRefundNo);
        RefundCreateResponse response = new RefundCreateResponse();
        response.setOutRefundNo(outRefundNo);
        response.setRefundId("MOCK_refund_" + randomHex(20));
        response.setRefundStatus(RefundStatus.SUCCESS);
        response.setSource("MOCK");
        return response;
    }

    private String randomHex(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
