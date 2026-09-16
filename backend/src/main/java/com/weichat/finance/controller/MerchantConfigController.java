package com.weichat.finance.controller;

import com.weichat.finance.common.R;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.enums.MerchantMode;
import com.weichat.finance.payment.client.WechatPayConfigManager;
import com.weichat.finance.service.MerchantConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 商户配置 Controller（v2.0.3 完整版）。
 *
 * <h3>API 列表</h3>
 * <ul>
 *   <li>{@code GET  /api/v1/merchant/{mchId}}                查询单个商户配置</li>
 *   <li>{@code GET  /api/v1/merchant}                       列出所有启用的商户</li>
 *   <li>{@code GET  /api/v1/merchant/partner/list}          列出所有启用的服务商</li>
 *   <li>{@code GET  /api/v1/merchant/{partnerMchId}/subs}   列出某服务商下的特约商户</li>
 *   <li>{@code POST /api/v1/merchant}                       创建商户（DIRECT 或 PARTNER）</li>
 *   <li>{@code PUT  /api/v1/merchant/{mchId}}               更新商户配置</li>
 *   <li>{@code DELETE /api/v1/merchant/{mchId}}             软删除商户</li>
 *   <li>{@code POST /api/v1/merchant/{mchId}/reload-config} 手动预加载商户 Config</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-14
 */
@RestController
@RequestMapping("/v1/merchant")
@Tag(name = "商户配置", description = "商户配置 CRUD + 服务商进件")
public class MerchantConfigController {

    private static final Logger log = LoggerFactory.getLogger(MerchantConfigController.class);

    private final MerchantConfigService merchantConfigService;
    private final WechatPayConfigManager configManager;

    @Autowired
    public MerchantConfigController(MerchantConfigService merchantConfigService,
                                     WechatPayConfigManager configManager) {
        this.merchantConfigService = merchantConfigService;
        this.configManager = configManager;
    }

    /**
     * 根据商户号查询商户配置。
     */
    @Operation(summary = "查询商户配置", description = "根据商户号查询商户配置信息")
    @GetMapping("/{mchId}")
    public R<MerchantConfig> getByMchId(@Parameter(description = "商户号") @PathVariable String mchId) {
        log.info("查询商户配置 mchId={}", mchId);
        MerchantConfig config = merchantConfigService.getByMchId(mchId);
        if (config == null) {
            return R.fail(404, "商户不存在：" + mchId);
        }
        return R.ok(config);
    }

    /**
     * 列出所有启用的商户。
     */
    @Operation(summary = "列出所有启用的商户", description = "查询所有 enabled=1 的商户配置")
    @GetMapping
    public R<List<MerchantConfig>> listEnabled() {
        List<MerchantConfig> list = merchantConfigService.lambdaQuery()
            .eq(MerchantConfig::getEnabled, 1)
            .eq(MerchantConfig::getIsDeleted, 0)
            .list();
        return R.ok(list == null ? Collections.emptyList() : list);
    }

    /**
     * 列出所有启用的服务商（mode=PARTNER）。
     */
    @Operation(summary = "列出所有启用的服务商", description = "查询 mode=PARTNER 且 enabled=1 的服务商")
    @GetMapping("/partner/list")
    public R<List<MerchantConfig>> listPartners() {
        List<MerchantConfig> list = merchantConfigService.listEnabledPartnerMerchants();
        return R.ok(list == null ? Collections.emptyList() : list);
    }

    /**
     * 列出某服务商下的所有特约商户。
     */
    @Operation(summary = "列出某服务商下的特约商户",
        description = "根据服务商号 parent_mch_id 查询特约商户列表")
    @GetMapping("/{partnerMchId}/subs")
    public R<List<MerchantConfig>> listSubMerchants(
            @Parameter(description = "服务商号") @PathVariable String partnerMchId) {
        log.info("列出服务商下的特约商户: partnerMchId={}", partnerMchId);
        List<MerchantConfig> list = merchantConfigService.listSubMerchants(partnerMchId);
        return R.ok(list == null ? Collections.emptyList() : list);
    }

    /**
     * 创建商户（DIRECT 或 PARTNER）。
     */
    @Operation(summary = "创建商户配置",
        description = "创建商户（DIRECT 直连商户 / PARTNER 服务商或特约商户）")
    @PostMapping
    public R<MerchantConfig> create(@Valid @RequestBody MerchantConfig merchant) {
        log.info("创建商户: mchId={}, mode={}", merchant.getMchId(), merchant.getMode());
        try {
            MerchantConfig created = merchantConfigService.createMerchant(merchant);
            return R.ok(created);
        } catch (IllegalArgumentException e) {
            return R.fail(400, e.getMessage());
        }
    }

    /**
     * 更新商户配置。
     */
    @Operation(summary = "更新商户配置", description = "根据 mch_id 更新商户配置（不允许改 mch_id）")
    @PutMapping("/{mchId}")
    public R<Boolean> update(@Parameter(description = "商户号") @PathVariable String mchId,
                              @Valid @RequestBody MerchantConfig merchant) {
        log.info("更新商户: mchId={}", mchId);
        merchant.setMchId(mchId);  // 强制使用 path variable
        try {
            boolean ok = merchantConfigService.updateMerchant(merchant);
            return ok ? R.ok(true) : R.fail(500, "更新失败");
        } catch (IllegalArgumentException e) {
            return R.fail(400, e.getMessage());
        }
    }

    /**
     * 软删除商户。
     */
    @Operation(summary = "软删除商户", description = "根据 mch_id 软删除商户（is_deleted=1）")
    @DeleteMapping("/{mchId}")
    public R<Boolean> delete(@Parameter(description = "商户号") @PathVariable String mchId) {
        log.info("删除商户: mchId={}", mchId);
        boolean ok = merchantConfigService.deleteMerchant(mchId);
        return ok ? R.ok(true) : R.fail(404, "商户不存在或已删除");
    }

    /**
     * 手动预加载商户 Config（PARTNER 模式启动期或新商户进件后调用）。
     */
    @Operation(summary = "手动预加载商户 Config",
        description = "PARTNER 模式启动期或新商户进件后调用，把商户 Config 加载到内存缓存")
    @PostMapping("/{mchId}/reload-config")
    public R<String> reloadConfig(@Parameter(description = "商户号") @PathVariable String mchId) {
        log.info("手动预加载 Config: mchId={}", mchId);
        MerchantConfig merchant = merchantConfigService.getByMchId(mchId);
        if (merchant == null) {
            return R.fail(404, "商户不存在：" + mchId);
        }
        if (!MerchantMode.PARTNER.equals(merchant.getMode())) {
            return R.fail(400, "仅 PARTNER 模式需要预加载（DIRECT 模式启动期已自动加载）");
        }
        try {
            configManager.preloadPartnerConfig(merchant);
            return R.ok("Config 已预加载，缓存大小=" + configManager.cacheSize());
        } catch (Exception e) {
            log.error("预加载 Config 失败: mchId={}, error={}", mchId, e.getMessage(), e);
            return R.fail(500, "预加载失败：" + e.getMessage());
        }
    }

    /**
     * 清空 Config 缓存（仅用于调试）。
     */
    @Operation(summary = "清空 Config 缓存（调试用）", description = "清空所有缓存的 Config 和 Parser")
    @PostMapping("/cache/clear")
    public R<String> clearCache() {
        configManager.clearCache();
        log.warn("[MerchantConfig] Config 缓存已清空（调试操作）");
        return R.ok("Config 缓存已清空");
    }
}
