package com.weichat.finance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weichat.finance.entity.MerchantConfig;

import java.util.List;

/**
 * 商户配置 Service
 *
 * <h3>v2.0.3 升级</h3>
 * <p>支持 PARTNER 模式（服务商 + 特约商户）的 CRUD：</p>
 * <ul>
 *   <li>{@link #listEnabledPartnerMerchants()}：启动期批量预加载服务商 Config</li>
 *   <li>{@link #listSubMerchants(String)}：查询某服务商下的所有特约商户</li>
 *   <li>{@link #createMerchant(MerchantConfig)}：创建商户（DIRECT 或 PARTNER）</li>
 *   <li>{@link #updateMerchant(MerchantConfig)}：更新商户配置</li>
 *   <li>{@link #deleteMerchant(String)}：软删除商户</li>
 * </ul>
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

    /**
     * v2.0.3：列出所有启用的服务商（mode=PARTNER 且 enabled=1）。
     *
     * <p>用途：应用启动期通过 {@code WechatPayConfigManager.preloadPartnerConfig()}
     * 批量预加载服务商 Config。</p>
     *
     * @return 服务商列表
     */
    List<MerchantConfig> listEnabledPartnerMerchants();

    /**
     * v2.0.3：列出某服务商下的所有特约商户（mode=PARTNER 且 parent_mch_id=partnerMchId）。
     *
     * @param partnerMchId 服务商号
     * @return 特约商户列表
     */
    List<MerchantConfig> listSubMerchants(String partnerMchId);

    /**
     * v2.0.3：创建商户（DIRECT 或 PARTNER）。
     *
     * <p>校验：</p>
     * <ul>
     *   <li>mch_id 唯一</li>
     *   <li>PARTNER 模式必须有 parent_mch_id + sub_app_id</li>
     *   <li>DIRECT 模式 parent_mch_id 必须为空</li>
     * </ul>
     *
     * @param merchant 商户配置
     * @return 创建后的实体（含自增 ID）
     * @throws IllegalArgumentException 校验失败
     */
    MerchantConfig createMerchant(MerchantConfig merchant);

    /**
     * v2.0.3：更新商户配置。
     *
     * @param merchant 商户配置（必须含 mchId）
     * @return 是否成功
     */
    boolean updateMerchant(MerchantConfig merchant);

    /**
     * v2.0.3：软删除商户（is_deleted=1）。
     *
     * @param mchId 商户号
     * @return 是否成功
     */
    boolean deleteMerchant(String mchId);
}
