package com.weichat.finance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weichat.finance.entity.MerchantConfig;

/**
 * 商户配置 Service
 *
 * @author panhw
 * @since 2026-09-14
 */
public interface MerchantConfigService extends IService<MerchantConfig> {

    /**
     * 按商户号查询
     *
     * @param mchId 商户号
     * @return 商户配置，找不到返回 null
     */
    MerchantConfig getByMchId(String mchId);
}
