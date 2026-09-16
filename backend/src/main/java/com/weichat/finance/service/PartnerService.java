package com.weichat.finance.service;

import com.weichat.finance.controller.dto.PartnerOnboardRequest;
import com.weichat.finance.entity.MerchantConfig;

import java.util.List;

/**
 * 服务商进件 Service（v2.0.5）。
 *
 * <h3>业务范围</h3>
 * <ul>
 *   <li>服务商注册（{@link #onboardPartner(PartnerOnboardRequest)}）</li>
 *   <li>服务商资料更新（{@link #updatePartnerProfile(String, MerchantConfig)}）</li>
 *   <li>服务商进件状态查询（{@link #getPartnerStatus(String)}）</li>
 *   <li>服务商下的子商户进件（{@link #onboardSubMerchant(String, PartnerOnboardRequest)}）</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-16
 */
public interface PartnerService {

    /**
     * 服务商进件（注册）。
     *
     * <p>业务规则：</p>
     * <ol>
     *   <li>mchId 全局唯一（不可与现有商户/服务商冲突）</li>
     *   <li>cert_serial_no + cert_private_key_path 必填（用于回调验签）</li>
     *   <li>进件后默认 enabled=1（自动启用，可后续人工审核禁用）</li>
     *   <li>进件成功后<strong>不自动预加载 Config</strong>，由调用方决定何时手动预加载</li>
     * </ol>
     *
     * @param request 服务商进件请求
     * @return 进件成功后的 MerchantConfig 实体（含 id）
     * @throws IllegalArgumentException 校验失败或 mchId 已存在
     */
    MerchantConfig onboardPartner(PartnerOnboardRequest request);

    /**
     * 更新服务商资料。
     *
     * @param partnerMchId 服务商号
     * @param update       更新的字段（不含 mch_id）
     * @return 更新后的实体
     */
    MerchantConfig updatePartnerProfile(String partnerMchId, MerchantConfig update);

    /**
     * 查询服务商进件状态。
     *
     * @param partnerMchId 服务商号
     * @return 服务商配置（含 enabled, mode 等）
     */
    MerchantConfig getPartnerStatus(String partnerMchId);

    /**
     * 服务商下的子商户（特约商户）进件。
     *
     * <p>与服务商进件的区别：</p>
     * <ul>
     *   <li>mchId = 特约商户号（不是服务商号）</li>
     *   <li>parent_mch_id = 服务商号</li>
     *   <li>sub_app_id = 特约商户 AppID</li>
     * </ul>
     *
     * @param partnerMchId 服务商号
     * @param request      子商户进件请求（必填 sub_app_id，mch_id = 特约商户号）
     * @return 子商户实体
     */
    MerchantConfig onboardSubMerchant(String partnerMchId, PartnerOnboardRequest request);

    /**
     * 列出某服务商下的所有子商户。
     *
     * @param partnerMchId 服务商号
     * @return 子商户列表
     */
    List<MerchantConfig> listSubMerchants(String partnerMchId);
}
