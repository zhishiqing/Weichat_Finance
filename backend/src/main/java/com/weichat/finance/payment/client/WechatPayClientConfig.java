package com.weichat.finance.payment.client;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
// 提示：v0.2.12 SDK 类实际名为 JsapiService（不是 JsapiServiceSdk）
import com.weichat.finance.payment.config.WechatPayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 微信支付 V3 客户端 Bean 配置。
 *
 * <p>仅在 wechatpay.mode=REAL 时激活（用真实凭证：商户私钥）。</p>
 *
 * <p>依赖 wechatpay-java SDK（com.github.wechatpay-apiv3:wechatpay-java）。</p>
 *
 * <p>v0.2.12 SDK 的 RSAAutoCertificateConfig / NotificationConfig 配置流程比较繁琐
 * （涉及 3 个 Builder 串接：RSAConfig + CertificateProvider + AeadCipher），此处暂时
 * 占位，等待 Phase 3 真实验证时再激活完整实现。</p>
 *
 * <p>Phase 2.1 待办：完整实现 Config + JsapiServiceSdk + NotificationParser Bean。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
// @Configuration
// @RequiredArgsConstructor
// @ConditionalOnProperty(prefix = "wechatpay", name = "mode", havingValue = "REAL", matchIfMissing = false)
public class WechatPayClientConfig {

    /**
     * 加载商户私钥（从 PEM 文件）。Phase 2.1 使用。
     */
    @SuppressWarnings("unused")
    private java.util.function.Supplier<String> loadMerchantPrivateKeySupplier;

    public WechatPayClientConfig(com.weichat.finance.payment.config.WechatPayProperties properties) {
        this.loadMerchantPrivateKeySupplier = () -> {
            try {
                java.nio.file.Path path = java.nio.file.Path.of(
                    properties.getMerchant().getCert().getPrivateKeyPath());
                if (!java.nio.file.Files.exists(path)) {
                    throw new IllegalStateException(
                        "商户私钥文件不存在: " + path.toAbsolutePath()
                        + "。请将 apiclient_key.pem 放到该路径，或切换 wechatpay.mode=MOCK");
                }
                return java.nio.file.Files.readString(path);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    /*
     * Phase 2.1 实现细节（保留作为参考）：
     *
     * @Bean
     * public com.wechat.pay.java.core.Config wxpayConfig() throws IOException {
     *     return new com.wechat.pay.java.core.RSAAutoCertificateConfig.Builder()
     *         .merchantId(properties.getMerchant().getMchId())
     *         .appId(properties.getMerchant().getAppId())
     *         .merchantSerialNumber(properties.getMerchant().getCert().getSerialNo())
     *         .privateKey(loadMerchantPrivateKey())
     *         .apiV3Key(properties.getMerchant().getApiV3Key())
     *         .build();
     * }
     *
     * @Bean
     * public com.wechat.pay.java.service.payments.jsapi.JsapiServiceSdk
     *     jsapiServiceSdk(com.wechat.pay.java.core.Config config) {
     *     return new com.wechat.pay.java.service.payments.jsapi.JsapiServiceSdk.Builder()
     *         .config(config)
     *         .build();
     * }
     *
     * @Bean
     * public com.wechat.pay.java.core.notification.NotificationParser
     *     notificationParser() {
     *     // 详见 v0.2.12 README
     * }
     */
}
