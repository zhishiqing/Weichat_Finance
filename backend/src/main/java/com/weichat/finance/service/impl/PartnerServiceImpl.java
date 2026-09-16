package com.weichat.finance.service.impl;

import com.weichat.finance.controller.dto.PartnerOnboardRequest;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.enums.CommonFlag;
import com.weichat.finance.entity.enums.MerchantMode;
import com.weichat.finance.payment.client.WechatPayConfigManager;
import com.weichat.finance.service.MerchantConfigService;
import com.weichat.finance.service.PartnerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 服务商进件 Service 实现（v2.0.5）。
 *
 * @author panhw
 * @since 2026-09-16
 */
@Service
public class PartnerServiceImpl implements PartnerService {

    private static final Logger log = LoggerFactory.getLogger(PartnerServiceImpl.class);

    @Autowired
    private MerchantConfigService merchantConfigService;

    @Autowired
    private WechatPayConfigManager configManager;

    @Override
    public MerchantConfig onboardPartner(PartnerOnboardRequest request) {
        log.info("[Partner] 服务商进件: mchId={}, merchantName={}", request.getMchId(), request.getMerchantName());

        MerchantConfig merchant = new MerchantConfig();
        merchant.setMchId(request.getMchId());
        merchant.setAppId(request.getAppId());
        merchant.setMerchantName(request.getMerchantName());
        merchant.setMode(MerchantMode.PARTNER);
        merchant.setApiV3Key(request.getApiV3Key());
        merchant.setCertSerialNo(request.getCertSerialNo());
        merchant.setCertPrivateKeyPath(request.getCertPrivateKeyPath());
        merchant.setNotifyUrlBase(request.getNotifyUrlBase());
        merchant.setEnabled(CommonFlag.ENABLED);

        // 联系人 + 资质信息存 ext 字段（JSON 格式）
        merchant.setExt(buildExtJson(request));

        return merchantConfigService.createMerchant(merchant);
    }

    @Override
    public MerchantConfig updatePartnerProfile(String partnerMchId, MerchantConfig update) {
        MerchantConfig existing = merchantConfigService.getByMchId(partnerMchId);
        if (existing == null) {
            throw new IllegalArgumentException("服务商不存在：" + partnerMchId);
        }
        if (!MerchantMode.PARTNER.equals(existing.getMode())) {
            throw new IllegalArgumentException("该商户不是服务商：" + partnerMchId);
        }

        // 允许更新的字段
        if (update.getMerchantName() != null) existing.setMerchantName(update.getMerchantName());
        if (update.getAppId() != null) existing.setAppId(update.getAppId());
        if (update.getNotifyUrlBase() != null) existing.setNotifyUrlBase(update.getNotifyUrlBase());
        if (update.getApiV3Key() != null) existing.setApiV3Key(update.getApiV3Key());
        if (update.getCertSerialNo() != null) existing.setCertSerialNo(update.getCertSerialNo());
        if (update.getCertPrivateKeyPath() != null) existing.setCertPrivateKeyPath(update.getCertPrivateKeyPath());
        if (update.getEnabled() != null) existing.setEnabled(update.getEnabled());

        boolean ok = merchantConfigService.updateMerchant(existing);
        log.info("[Partner] 更新服务商资料: mchId={}, ok={}", partnerMchId, ok);
        return existing;
    }

    @Override
    public MerchantConfig getPartnerStatus(String partnerMchId) {
        MerchantConfig partner = merchantConfigService.getByMchId(partnerMchId);
        if (partner == null) {
            return null;
        }
        if (!MerchantMode.PARTNER.equals(partner.getMode())) {
            throw new IllegalArgumentException("该商户不是服务商：" + partnerMchId);
        }
        return partner;
    }

    @Override
    public MerchantConfig onboardSubMerchant(String partnerMchId, PartnerOnboardRequest request) {
        // 1. 校验服务商存在
        MerchantConfig partner = merchantConfigService.getByMchId(partnerMchId);
        if (partner == null) {
            throw new IllegalArgumentException("服务商不存在：" + partnerMchId);
        }
        if (!MerchantMode.PARTNER.equals(partner.getMode())) {
            throw new IllegalArgumentException("该商户不是服务商：" + partnerMchId);
        }
        if (CommonFlag.DISABLED == partner.getEnabled()) {
            throw new IllegalArgumentException("服务商已禁用，无法进件子商户：" + partnerMchId);
        }

        log.info("[Partner] 子商户进件: partnerMchId={}, subMchId={}, subAppId={}",
            partnerMchId, request.getMchId(), request.getAppId());

        MerchantConfig sub = new MerchantConfig();
        sub.setMchId(request.getMchId());
        sub.setAppId(request.getAppId());
        sub.setMerchantName(request.getMerchantName());
        sub.setMode(MerchantMode.PARTNER);
        sub.setParentMchId(partnerMchId);
        sub.setSubAppId(request.getAppId());  // 子商户 AppID 与 sub_app_id 字段一致
        sub.setApiV3Key(request.getApiV3Key());
        sub.setCertSerialNo(request.getCertSerialNo());
        sub.setCertPrivateKeyPath(request.getCertPrivateKeyPath());
        sub.setNotifyUrlBase(request.getNotifyUrlBase());
        sub.setEnabled(CommonFlag.ENABLED);
        sub.setExt(buildExtJson(request));

        return merchantConfigService.createMerchant(sub);
    }

    @Override
    public List<MerchantConfig> listSubMerchants(String partnerMchId) {
        return merchantConfigService.listSubMerchants(partnerMchId);
    }

    /**
     * 把联系人 + 资质信息打包成 JSON ext。
     *
     * <p>v2.0.5 简化实现：手写 JSON（避免引入 fastjson 依赖）。
     * 生产环境建议用 Jackson ObjectMapper。</p>
     */
    private String buildExtJson(PartnerOnboardRequest req) {
        StringBuilder sb = new StringBuilder("{");
        appendIfNotNull(sb, "contactName", req.getContactName(), true);
        appendIfNotNull(sb, "contactPhone", req.getContactPhone(), false);
        appendIfNotNull(sb, "contactEmail", req.getContactEmail(), false);
        appendIfNotNull(sb, "businessLicenseNo", req.getBusinessLicenseNo(), false);
        appendIfNotNull(sb, "bankAccountNo", req.getBankAccountNo(), false);
        appendIfNotNull(sb, "bankAccountName", req.getBankAccountName(), false);
        appendIfNotNull(sb, "bankName", req.getBankName(), false);
        sb.append("}");
        return sb.toString();
    }

    private void appendIfNotNull(StringBuilder sb, String key, String value, boolean first) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (!first) {
            sb.append(",");
        }
        sb.append("\"").append(key).append("\":\"").append(value).append("\"");
    }
}
