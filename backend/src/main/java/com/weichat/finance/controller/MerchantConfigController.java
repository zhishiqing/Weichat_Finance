package com.weichat.finance.controller;

import com.weichat.finance.common.R;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.service.MerchantConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商户配置 Controller（示例）。
 *
 * @author panhw
 * @since 2026-09-14
 */
@RestController
@RequestMapping("/v1/merchant")
@Tag(name = "商户配置", description = "商户配置查询（示例接口）")
public class MerchantConfigController {

    private static final Logger log = LoggerFactory.getLogger(MerchantConfigController.class);

    private final MerchantConfigService merchantConfigService;

    public MerchantConfigController(MerchantConfigService merchantConfigService) {
        this.merchantConfigService = merchantConfigService;
    }

    /**
     * 根据商户号查询商户配置。
     *
     * @param mchId 商户号
     * @return 商户配置
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
}
