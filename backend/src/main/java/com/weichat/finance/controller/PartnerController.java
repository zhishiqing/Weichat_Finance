package com.weichat.finance.controller;

import com.weichat.finance.common.R;
import com.weichat.finance.controller.dto.MerchantConfigResponse;
import com.weichat.finance.controller.dto.PartnerOnboardRequest;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.payment.client.WechatPayConfigManager;
import com.weichat.finance.service.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务商进件 Controller（v2.0.5）。
 *
 * <h3>API 列表</h3>
 * <ul>
 *   <li>{@code POST /api/v1/partner/onboard}              服务商进件（注册）</li>
 *   <li>{@code PUT  /api/v1/partner/{partnerMchId}}       更新服务商资料</li>
 *   <li>{@code GET  /api/v1/partner/{partnerMchId}}       查询服务商进件状态</li>
 *   <li>{@code POST /api/v1/partner/{partnerMchId}/sub/onboard}  子商户进件</li>
 *   <li>{@code GET  /api/v1/partner/{partnerMchId}/sub/list}     列出子商户</li>
 *   <li>{@code POST /api/v1/partner/{partnerMchId}/reload-config} 手动预加载 Config</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-16
 */
@RestController
@RequestMapping("/v1/partner")
@Tag(name = "服务商进件", description = "服务商 / 特约商户进件（v2.0.5）")
public class PartnerController {

    private static final Logger log = LoggerFactory.getLogger(PartnerController.class);

    private final PartnerService partnerService;
    private final WechatPayConfigManager configManager;

    @Autowired
    public PartnerController(PartnerService partnerService, WechatPayConfigManager configManager) {
        this.partnerService = partnerService;
        this.configManager = configManager;
    }

    /**
     * 服务商进件（注册）。
     *
     * <p>注意：进件成功后需要再调用 {@link #reloadConfig(String)} 手动预加载 Config，
     * 否则该服务商的回调路由无法正常工作。</p>
     */
    @Operation(summary = "服务商进件",
        description = "服务商注册接口。返回完整实体（含敏感字段），仅创建时返回。")
    @PostMapping("/onboard")
    public R<MerchantConfig> onboard(@Valid @RequestBody PartnerOnboardRequest request) {
        log.info("服务商进件: mchId={}, merchantName={}", request.getMchId(), request.getMerchantName());
        try {
            MerchantConfig partner = partnerService.onboardPartner(request);
            return R.ok(partner);
        } catch (IllegalArgumentException e) {
            return R.fail(400, e.getMessage());
        }
    }

    /**
     * 更新服务商资料。
     */
    @Operation(summary = "更新服务商资料",
        description = "根据服务商号更新资料（不含 mch_id）")
    @PutMapping("/{partnerMchId}")
    public R<MerchantConfigResponse> update(
            @Parameter(description = "服务商号") @PathVariable String partnerMchId,
            @RequestBody MerchantConfig update) {
        log.info("更新服务商资料: partnerMchId={}", partnerMchId);
        try {
            MerchantConfig updated = partnerService.updatePartnerProfile(partnerMchId, update);
            return R.ok(MerchantConfigResponse.mask(updated));
        } catch (IllegalArgumentException e) {
            return R.fail(400, e.getMessage());
        }
    }

    /**
     * 查询服务商进件状态（脱敏版）。
     */
    @Operation(summary = "查询服务商进件状态（脱敏）",
        description = "根据服务商号查询进件状态（敏感字段已脱敏）")
    @GetMapping("/{partnerMchId}")
    public R<MerchantConfigResponse> getStatus(
            @Parameter(description = "服务商号") @PathVariable String partnerMchId) {
        MerchantConfig partner = partnerService.getPartnerStatus(partnerMchId);
        if (partner == null) {
            return R.fail(404, "服务商不存在：" + partnerMchId);
        }
        return R.ok(MerchantConfigResponse.mask(partner));
    }

    /**
     * 子商户（特约商户）进件。
     */
    @Operation(summary = "特约商户进件",
        description = "在某服务商下进件特约商户（PARTNER 模式子商户）")
    @PostMapping("/{partnerMchId}/sub/onboard")
    public R<MerchantConfig> onboardSub(
            @Parameter(description = "服务商号") @PathVariable String partnerMchId,
            @Valid @RequestBody PartnerOnboardRequest request) {
        log.info("子商户进件: partnerMchId={}, subMchId={}", partnerMchId, request.getMchId());
        try {
            MerchantConfig sub = partnerService.onboardSubMerchant(partnerMchId, request);
            return R.ok(sub);
        } catch (IllegalArgumentException e) {
            return R.fail(400, e.getMessage());
        }
    }

    /**
     * 列出某服务商下的所有子商户（脱敏版）。
     */
    @Operation(summary = "列出服务商下的子商户（脱敏）",
        description = "根据服务商号查询特约商户列表（敏感字段已脱敏）")
    @GetMapping("/{partnerMchId}/sub/list")
    public R<List<MerchantConfigResponse>> listSubs(
            @Parameter(description = "服务商号") @PathVariable String partnerMchId) {
        List<MerchantConfig> list = partnerService.listSubMerchants(partnerMchId);
        if (list == null) {
            return R.ok(Collections.emptyList());
        }
        return R.ok(list.stream()
            .map(MerchantConfigResponse::mask)
            .collect(Collectors.toList()));
    }

    /**
     * 手动预加载服务商 Config。
     *
     * <p>建议在服务商进件成功后立即调用，让回调路由立即可用。</p>
     */
    @Operation(summary = "手动预加载服务商 Config",
        description = "把服务商 Config 加载到内存缓存（建议进件后立即调用）")
    @PostMapping("/{partnerMchId}/reload-config")
    public R<String> reloadConfig(@Parameter(description = "服务商号") @PathVariable String partnerMchId) {
        log.info("手动预加载 Config: partnerMchId={}", partnerMchId);
        MerchantConfig partner = partnerService.getPartnerStatus(partnerMchId);
        if (partner == null) {
            return R.fail(404, "服务商不存在：" + partnerMchId);
        }
        try {
            configManager.preloadPartnerConfig(partner);
            return R.ok("Config 已预加载，缓存大小=" + configManager.cacheSize());
        } catch (Exception e) {
            log.error("预加载 Config 失败: mchId={}, error={}", partnerMchId, e.getMessage(), e);
            return R.fail(500, "预加载失败：" + e.getMessage());
        }
    }
}
