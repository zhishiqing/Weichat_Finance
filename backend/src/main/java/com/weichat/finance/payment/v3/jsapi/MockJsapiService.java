package com.weichat.finance.payment.v3.jsapi;

import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.payment.v3.jsapi.request.JsapiCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Mock JSAPI 服务（默认）。
 *
 * <p>用于本地开发与单元测试，<strong>不发起真实 HTTP 请求</strong>。</p>
 *
 * <p>生成的 prepay_id 格式：<code>MOCK_prepay_{32位hex}</code>。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Service
@ConditionalOnProperty(prefix = "wechatpay", name = "mode", havingValue = "MOCK", matchIfMissing = true)
public class MockJsapiService implements JsapiService {

    private static final Logger log = LoggerFactory.getLogger(MockJsapiService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "abcdef0123456789";

    @Override
    public JsapiCreateResponse create(JsapiCreateRequest request, MerchantConfig merchant) {
        log.warn("当前使用 MockJsapiService，下单不会到达微信支付服务端，仅用于本地调试。");
        log.info("[MOCK] 创建 JSAPI 订单: outTradeNo={}, amountTotal={}分, openid={}",
            request.getOutTradeNo(), request.getAmountTotal(), request.getOpenid());

        StringBuilder sb = new StringBuilder("MOCK_prepay_");
        for (int i = 0; i < 32; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }

        JsapiCreateResponse response = new JsapiCreateResponse();
        response.setPrepayId(sb.toString());
        response.setSource("MOCK");
        log.info("[MOCK] 返回 prepay_id={}", response.getPrepayId());
        return response;
    }
}
