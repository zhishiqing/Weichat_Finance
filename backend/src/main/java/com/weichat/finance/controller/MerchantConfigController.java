package com.weichat.finance.controller;

import com.weichat.finance.common.R;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.service.MerchantConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商户配置 Controller（示例）
 *
 * <p>演示完整链路：Controller → Service → Mapper → MySQL。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Slf4j
@RestController
@RequestMapping("/v1/merchant")
@RequiredArgsConstructor
public class MerchantConfigController {

    private final MerchantConfigService merchantConfigService;

    /**
     * 根据商户号查询商户配置
     *
     * @param mchId 商户号
     * @return 商户配置
     */
    @GetMapping("/{mchId}")
    public R<MerchantConfig> getByMchId(@PathVariable String mchId) {
        log.info("查询商户配置 mchId={}", mchId);
        MerchantConfig config = merchantConfigService.getByMchId(mchId);
        if (config == null) {
            return R.fail(404, "商户不存在：" + mchId);
        }
        return R.ok(config);
    }
}
