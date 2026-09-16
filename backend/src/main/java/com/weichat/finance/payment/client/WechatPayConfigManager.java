package com.weichat.finance.payment.client;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.enums.MerchantMode;
import com.weichat.finance.payment.config.WechatPayProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 微信支付 V3 多 Config 缓存管理器（服务商模式核心）。
 *
 * <h3>设计动机</h3>
 * <p>v1.6 之前：单 Config（{@link WechatPayProperties} 注入），只能支持单一商户号。</p>
 * <p>v2.0 服务商模式：需要支持</p>
 * <ul>
 *   <li>多个 <strong>服务商</strong>（每个服务商 1 个 Config）</li>
 *   <li>每个服务商下挂多个 <strong>特约商户</strong>（业务请求传 sub_mch_id）</li>
 *   <li>每个特约商户可能是 PARTNER 模式（挂载在某服务商下）或 DIRECT 模式（独立 Config）</li>
 * </ul>
 *
 * <h3>Config 路由策略</h3>
 * <pre>
 * DIRECT 模式商户  → 用商户自身 mch_id 查 Config 缓存
 * PARTNER 模式商户 → 用 parent_mch_id（服务商号）查 Config 缓存
 * </pre>
 *
 * <h3>缓存策略</h3>
 * <ul>
 *   <li>程序启动时：预加载 {@code wechatpay.merchant} 配置的默认 Config（兼容 v1.6 单商户）</li>
 *   <li>运行时：通过 {@link #getOrCreateConfig(String)} 懒加载其他商户 Config</li>
 *   <li>ConcurrentHashMap 缓存，避免重复创建（Config 内部包含证书缓存，重复创建会强制刷新）</li>
 * </ul>
 *
 * <h3>未来扩展</h3>
 * <ul>
 *   <li>v2.0：支持服务商模式（PARTNER）</li>
 *   <li>v2.1：付款码支付（V2）</li>
 *   <li>v3.0：多渠道（微信 / 支付宝 / 银联）</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-16
 */
@Component
@ConditionalOnProperty(prefix = "wechatpay", name = "mode", havingValue = "REAL")
public class WechatPayConfigManager {

    private static final Logger log = LoggerFactory.getLogger(WechatPayConfigManager.class);

    @Autowired
    private WechatPayProperties properties;

    /**
     * 默认 mchId（来自 {@code wechatpay.merchant.mchId} 配置）。
     *
     * <p>v1.6 启动期注入，运行时只读。用于回调解密时定位默认 Parser。</p>
     */
    private String defaultMchId;

    /**
     * Config 缓存：key = mchId（直连商户或服务商号）
     */
    private final ConcurrentMap<String, Config> configCache = new ConcurrentHashMap<>();

    /**
     * 初始化：把默认 DIRECT 商户 Config 加载到缓存（兼容 v1.6 单商户）。
     *
     * <p>启动期加载，避免首笔交易时冷启动耗时。</p>
     */
    @PostConstruct
    public void initDefaultConfig() {
        WechatPayProperties.Merchant m = properties.getMerchant();
        if (m == null || m.getMchId() == null) {
            log.warn("[ConfigManager] wechatpay.merchant.mchId 未配置，跳过默认 Config 初始化");
            return;
        }
        defaultMchId = m.getMchId();
        try {
            Config cfg = createConfig(
                m.getMchId(),
                m.getCert().getSerialNo(),
                m.getApiV3Key(),
                m.getCert().getPrivateKeyPath()
            );
            configCache.put(m.getMchId(), cfg);
            log.info("[ConfigManager] ✅ 默认 Config 已加载: mchId={}, certSerial={}",
                m.getMchId(), m.getCert().getSerialNo());
        } catch (Exception e) {
            log.error("[ConfigManager] 默认 Config 初始化失败: {}", e.getMessage(), e);
            // 不抛异常：保持启动成功，让后续运行时按需懒加载
        }
    }

    /**
     * 获取默认 mchId（用于回调解密路由）。
     */
    public String getDefaultMchId() {
        return defaultMchId;
    }

    /**
     * 获取所有已缓存的 mchId（用于回调遍历重试）。
     */
    public java.util.Set<String> getAllCachedMchIds() {
        return java.util.Collections.unmodifiableSet(configCache.keySet());
    }

    /**
     * 按 mchId 获取 Config（缓存未命中则从数据库查配置懒加载）。
     *
     * @param mchId 商户号（DIRECT 用自身 mchId，PARTNER 用服务商号 parentMchId）
     * @return SDK Config
     */
    public Config getOrCreateConfig(String mchId) {
        if (mchId == null) {
            throw new IllegalArgumentException("mchId 不能为空");
        }
        return configCache.computeIfAbsent(mchId, this::loadConfigFromDb);
    }

    /**
     * 按商户配置获取 Config（自动判断 DIRECT/PARTNER）。
     *
     * @param merchant 商户配置
     * @return SDK Config
     */
    public Config getConfigForMerchant(MerchantConfig merchant) {
        if (merchant == null) {
            throw new IllegalArgumentException("merchant 不能为空");
        }
        if (MerchantMode.PARTNER.equals(merchant.getMode())) {
            // PARTNER 模式：用服务商号查 Config
            if (merchant.getParentMchId() == null) {
                throw new IllegalStateException(
                    "PARTNER 模式商户必须配置 parentMchId: mchId=" + merchant.getMchId());
            }
            return getOrCreateConfig(merchant.getParentMchId());
        }
        // DIRECT 模式：用商户自身 mchId 查 Config
        return getOrCreateConfig(merchant.getMchId());
    }

    /**
     * 从数据库查商户配置 + 创建 Config。
     *
     * <p>v2.0 实现：从 {@code t_merchant_config} 查 mchId 对应的配置（certSerial / apiV3Key / privateKeyPath）。</p>
     * <p>v1.6 简化实现：直接报错，要求调用方先用 {@link #preloadPartnerConfig} 显式加载。</p>
     */
    private Config loadConfigFromDb(String mchId) {
        // v1.6 实现：尚未从 DB 懒加载（PARTNER 模式需要主动 preload）
        throw new UnsupportedOperationException(
            "[ConfigManager] mchId=" + mchId + " 的 Config 未预加载。"
            + "PARTNER 模式启动期需调用 WechatPayConfigManager.preloadPartnerConfig(...)"
            + " 主动加载服务商 Config。v1.6 暂未实现 DB 懒加载。");
    }

    /**
     * 显式预加载服务商 Config（PARTNER 模式启动期调用）。
     *
     * <p>使用示例（{@code CommandLineRunner}）：</p>
     * <pre>{@code
     * merchantConfigService.listEnabledPartnerMerchants()
     *     .forEach(m -> {
     *         MerchantConfig partner = merchantConfigService.getByMchId(m.getParentMchId());
     *         if (partner != null) {
     *             configManager.preloadPartnerConfig(partner);
     *         }
     *     });
     * }</pre>
     *
     * @param partner 服务商配置（mode=PARTNER, mch_id=服务商号, cert_serial=服务商证书）
     */
    public void preloadPartnerConfig(MerchantConfig partner) {
        if (partner == null || !MerchantMode.PARTNER.equals(partner.getMode())) {
            return;
        }
        if (configCache.containsKey(partner.getMchId())) {
            log.debug("[ConfigManager] 服务商 Config 已存在，跳过: mchId={}", partner.getMchId());
            return;
        }
        try {
            Config cfg = createConfig(
                partner.getMchId(),
                partner.getCertSerialNo(),
                partner.getApiV3Key(),
                partner.getCertPrivateKeyPath()
            );
            configCache.put(partner.getMchId(), cfg);
            log.info("[ConfigManager] ✅ 服务商 Config 预加载成功: partnerMchId={}, certSerial={}",
                partner.getMchId(), partner.getCertSerialNo());
        } catch (Exception e) {
            log.error("[ConfigManager] 服务商 Config 预加载失败: mchId={}, error={}",
                partner.getMchId(), e.getMessage(), e);
        }
    }

    /**
     * 创建 SDK Config（RSA + 自动下载平台证书）。
     */
    private Config createConfig(String mchId, String certSerial, String apiV3Key, String privateKeyPath) {
        String privateKey = readPrivateKey(privateKeyPath);
        log.info("[ConfigManager] 创建 Config: mchId={}, certSerial={}, keyPath={}",
            mchId, certSerial, privateKeyPath);
        return new RSAAutoCertificateConfig.Builder()
            .merchantId(mchId)
            .merchantSerialNumber(certSerial)
            .apiV3Key(apiV3Key)
            .privateKey(privateKey)
            .build();
    }

    /**
     * 读取商户私钥 PEM 文件内容。
     */
    private String readPrivateKey(String rawPath) {
        try {
            Path p = Path.of(rawPath);
            if (!p.isAbsolute()) {
                p = Path.of(System.getProperty("user.dir"), rawPath);
            }
            if (!Files.exists(p)) {
                throw new IllegalStateException("商户私钥文件不存在: " + p.toAbsolutePath());
            }
            return Files.readString(p);
        } catch (IOException e) {
            throw new RuntimeException("读取商户私钥失败: " + rawPath + ": " + e.getMessage(), e);
        }
    }

    /**
     * 清空缓存（用于测试 / 配置变更后重置）。
     */
    public void clearCache() {
        configCache.clear();
        log.info("[ConfigManager] Config 缓存已清空");
    }

    /**
     * 当前已缓存的 Config 数量。
     */
    public int cacheSize() {
        return configCache.size();
    }
}
