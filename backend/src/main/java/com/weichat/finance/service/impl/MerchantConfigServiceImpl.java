package com.weichat.finance.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.mapper.MerchantConfigMapper;
import com.weichat.finance.service.MerchantConfigService;
import org.springframework.stereotype.Service;

/**
 * 商户配置 Service 实现
 *
 * @author panhw
 * @since 2026-09-14
 */
@Service
public class MerchantConfigServiceImpl extends ServiceImpl<MerchantConfigMapper, MerchantConfig>
        implements MerchantConfigService {

    @Override
    public MerchantConfig getByMchId(String mchId) {
        return this.getOne(Wrappers.<MerchantConfig>lambdaQuery().eq(MerchantConfig::getMchId, mchId));
    }
}
