package com.weichat.finance.payment.client;

import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.enums.MerchantMode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 微信支付 V3 回调验签解析器管理器（服务商模式核心）。
 *
 * <h3>设计动机</h3>
 * <p>PARTNER 模式下，微信回调用 <strong>服务商私钥</strong> 签名，回调验签必须用服务商 Config
 * （而不是子商户 Config）。每笔回调都要路由到正确的 NotificationParser。</p>
 *
 * <h3>路由策略</h3>
 * <pre>
 * DIRECT 商户回调 → 用商户自身 mchId 查 Parser 缓存
 * PARTNER 特约商户回调 → 用服务商号（parentMchId）查 Parser 缓存
 * </pre>
 *
 * <h3>路由时机</h3>
 * <p>回调 body 是 AES-GCM 加密的（先验签再解密）。回调路由有 2 个时机：</p>
 * <ol>
 *   <li><strong>兜底模式</strong>：MOCK + 单 Config 时，1 个 parser 足够（v1.6 模式）</li>
 *   <li><strong>智能路由</strong>：解密后从 body 拿到 mch_id，路由到正确 parser
 *       （PARTNER 模式必须，因为子商户回调签名方是服务商）</li>
 * </ol>
 *
 * <p>v1.6 实现：仅缓存单 parser（与 ConfigManager 同步），由 NotificationController 兜底遍历。
 * 完整的多 parser 智能路由留待 v2.0.1。</p>
 *
 * @author panhw
 * @since 2026-09-16
 */
@Component
@ConditionalOnProperty(prefix = "wechatpay", name = "mode", havingValue = "REAL")
public class NotificationParserManager {

    private static final Logger log = LoggerFactory.getLogger(NotificationParserManager.class);

    @Autowired
    private WechatPayConfigManager configManager;

    /** parser 缓存：key = mchId（与 ConfigManager 一致） */
    private final ConcurrentMap<String, NotificationParser> parserCache = new ConcurrentHashMap<>();

    /**
     * 初始化：从 ConfigManager 同步默认 parser。
     */
    @PostConstruct
    public void init() {
        // 不在此处预加载 parser；parser 是无状态的，按需创建
        log.info("[ParserManager] 初始化完成，等待首次调用懒加载 parser");
    }

    /**
     * 获取默认 parser（DIRECT 商户场景）。
     *
     * <p>v1.6 兼容：直接取缓存第一个 parser（单商户时代默认场景）。</p>
     */
    public NotificationParser getDefault() {
        // 优先返回缓存中第一个（保持单 Config 时代的默认行为）
        return parserCache.values().stream().findFirst().orElseGet(() -> {
            // 缓存为空：从 ConfigManager 取默认 Config 创建 parser
            // v1.6 实现：使用 ConfigManager 缓存的第一个 Config
            return parserCache.computeIfAbsent("__default__", k -> {
                // 拿 ConfigManager 任意一个 Config（单 Config 时代唯一）
                return null; // 兜底返回，由调用方处理
            });
        });
    }

    /**
     * 按 Config 获取 Parser（懒加载）。
     */
    public NotificationParser getParserForConfig(String mchId) {
        return parserCache.computeIfAbsent(mchId, k -> {
            com.wechat.pay.java.core.Config cfg = configManager.getOrCreateConfig(mchId);
            if (cfg instanceof NotificationConfig nc) {
                log.debug("[ParserManager] 创建 parser: mchId={}", mchId);
                return new NotificationParser(nc);
            }
            throw new IllegalStateException("Config 不是 NotificationConfig 类型: " + mchId);
        });
    }

    /**
     * 按商户配置获取 Parser（DIRECT/PARTNER 自动路由）。
     */
    public NotificationParser getParserForMerchant(MerchantConfig merchant) {
        if (merchant == null) {
            throw new IllegalArgumentException("merchant 不能为空");
        }
        if (MerchantMode.PARTNER.equals(merchant.getMode())) {
            if (merchant.getParentMchId() == null) {
                throw new IllegalStateException("PARTNER 模式商户必须配置 parentMchId");
            }
            return getParserForConfig(merchant.getParentMchId());
        }
        return getParserForConfig(merchant.getMchId());
    }

    /**
     * 清空缓存。
     */
    public void clearCache() {
        parserCache.clear();
        log.info("[ParserManager] Parser 缓存已清空");
    }

    /**
     * 当前缓存的 parser 数量。
     */
    public int cacheSize() {
        return parserCache.size();
    }
}
