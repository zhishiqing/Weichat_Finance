package com.weichat.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weichat.finance.entity.MerchantConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商户配置 Mapper
 *
 * <p>继承 BaseMapper 获得 CRUD，遵循 readme 6.5 约束（禁止裸写 SQL）。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Mapper
public interface MerchantConfigMapper extends BaseMapper<MerchantConfig> {
}
