package com.weichat.finance.payment.client;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.weichat.finance.payment.config.WechatPayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 微信支付 V3 客户端 Bean 配置（REAL 模式专用）。
 *
 * <h3>激活条件</h3>
 * {@code wechatpay.mode=REAL} 时激活，本类提供：
 * <ul>
 *   <li>{@link Config}：调用下单 / 查单 / 关单 / 退款 / 账单下载 所需的完整配置
 *     （商户号 + AppID + 私钥 + 证书序列号 + APIv3 密钥，启动时<strong>自动下载微信平台证书</strong>）</li>
 *   <li>{@link NotificationParser}：回调验签 + 资源解密</li>
 * </ul>
 *
 * <h3>运行时流程</h3>
 * <ol>
 *   <li>应用启动时调用 {@code GET /v3/certificates} 获取微信平台证书</li>
 *   <li>本地缓存证书到内存 + DB（{@code t_platform_cert} 表，预留）</li>
 *   <li>签名 / 验签 / 加密 / 解密 均依赖此配置</li>
 * </ol>
 *
 * <h3>安全提示</h3>
 * 商户私钥文件（apiclient_key.pem）必须妥善保管，建议生产环境通过
 * KMS / 加密卷轴挂载。本工程 .gitignore 已忽略 *.pem / backend/certs/。
 *
 * @author panhw
 * @since 2026-09-16
 */
@Configuration
@ConditionalOnProperty(prefix = "wechatpay", name = "mode", havingValue = "REAL")
public class WechatPayClientConfig {

    private static final Logger log = LoggerFactory.getLogger(WechatPayClientConfig.class);

    private final WechatPayProperties properties;

    public WechatPayClientConfig(WechatPayProperties properties) {
        this.properties = properties;
    }

    /**
     * 商户私钥 Supplier（懒加载：首次访问时读 PEM 文件）。
     */
    @Bean(name = "merchantPrivateKeySupplier")
    public java.util.function.Supplier<String> merchantPrivateKeySupplier() {
        return () -> {
            try {
                Path path = resolveKeyPath();
                if (!Files.exists(path)) {
                    throw new IllegalStateException(
                        "商户私钥文件不存在: " + path.toAbsolutePath()
                        + "。请将 apiclient_key.pem 放到该路径，或切换 wechatpay.mode=MOCK");
                }
                String content = Files.readString(path);
                log.debug("已加载商户私钥: path={}, length={}", path, content.length());
                return content;
            } catch (IOException e) {
                throw new RuntimeException("读取商户私钥失败: " + e.getMessage(), e);
            }
        };
    }

    /**
     * 微信支付 V3 SDK Config（含 RSA + 自动下载平台证书）。
     */
    @Bean
    public Config wechatPayConfig(java.util.function.Supplier<String> merchantPrivateKeySupplier) {
        WechatPayProperties.Merchant m = properties.getMerchant();
        log.info("[Real] 初始化微信支付 V3 Config: mchId={}, appId={}, certSerial={}",
            m.getMchId(), m.getAppId(), m.getCert().getSerialNo());

        try {
            return new RSAAutoCertificateConfig.Builder()
                .merchantId(m.getMchId())
                .merchantSerialNumber(m.getCert().getSerialNo())
                .apiV3Key(m.getApiV3Key())
                .privateKey(merchantPrivateKeySupplier.get())
                .build();
        } catch (Exception e) {
            log.error("[Real] 微信支付 Config 初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("初始化微信支付 Config 失败", e);
        }
    }

    /**
     * 回调验签 + 解密器（NotificationParser）。
     *
     * <p>RSAAutoCertificateConfig 本身实现了 NotificationConfig 接口，
     * 可直接传入 NotificationParser 构造函数。</p>
     */
    @Bean
    public NotificationParser notificationParser(Config wechatPayConfig) {
        log.info("[Real] 初始化 NotificationParser（回调验签+解密）");
        if (wechatPayConfig instanceof NotificationConfig notificationConfig) {
            return new NotificationParser(notificationConfig);
        }
        throw new IllegalStateException(
            "Config 不是 NotificationConfig 类型: " + wechatPayConfig.getClass().getName());
    }

    /**
     * 解析商户私钥路径（支持绝对路径 / 相对路径）。
     */
    private Path resolveKeyPath() {
        String rawPath = properties.getMerchant().getCert().getPrivateKeyPath();
        Path p = Path.of(rawPath);
        if (!p.isAbsolute()) {
            // 相对路径相对于项目根目录（包含 backend/）
            String userDir = System.getProperty("user.dir");
            // user.dir 通常是 backend/ 本身
            p = Path.of(userDir, rawPath);
        }
        return p;
    }
}
