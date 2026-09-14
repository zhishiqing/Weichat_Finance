package com.weichat.finance.payment.v3.nativepay;

import com.weichat.finance.entity.MerchantConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Mock Native 支付服务。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Service
@ConditionalOnProperty(prefix = "wechatpay", name = "mode", havingValue = "MOCK", matchIfMissing = true)
public class MockNativeService implements NativeService {

    private static final Logger log = LoggerFactory.getLogger(MockNativeService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Override
    public NativeCreateResponse create(NativeCreateRequest request, MerchantConfig merchant) {
        log.warn("当前使用 MockNativeService，下单不会到达微信支付服务端，仅用于本地调试。");
        log.info("[MOCK] Native 下单: outTradeNo={}, amountTotal={}分", request.getOutTradeNo(), request.getAmountTotal());

        String codeUrl = "weixin://wxpay/bizpayurl?pr=MOCK_" + randomString(20);
        NativeCreateResponse response = new NativeCreateResponse();
        response.setCodeUrl(codeUrl);
        response.setSource("MOCK");
        log.info("[MOCK] 返回 code_url={}", codeUrl);
        return response;
    }

    private String randomString(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
