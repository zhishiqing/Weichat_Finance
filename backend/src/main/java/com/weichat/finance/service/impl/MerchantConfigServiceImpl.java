package com.weichat.finance.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.enums.CommonFlag;
import com.weichat.finance.entity.enums.MerchantMode;
import com.weichat.finance.mapper.MerchantConfigMapper;
import com.weichat.finance.service.MerchantConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商户配置 Service 实现
 *
 * @author panhw
 * @since 2026-09-14
 */
@Service
public class MerchantConfigServiceImpl extends ServiceImpl<MerchantConfigMapper, MerchantConfig>
        implements MerchantConfigService {

    private static final Logger log = LoggerFactory.getLogger(MerchantConfigServiceImpl.class);

    @Override
    public MerchantConfig getByMchId(String mchId) {
        return this.getOne(Wrappers.<MerchantConfig>lambdaQuery()
            .eq(MerchantConfig::getMchId, mchId)
            .eq(MerchantConfig::getIsDeleted, CommonFlag.NOT_DELETED));
    }

    @Override
    public List<MerchantConfig> listEnabledPartnerMerchants() {
        return this.list(Wrappers.<MerchantConfig>lambdaQuery()
            .eq(MerchantConfig::getMode, MerchantMode.PARTNER)
            .eq(MerchantConfig::getEnabled, CommonFlag.ENABLED)
            .eq(MerchantConfig::getIsDeleted, CommonFlag.NOT_DELETED));
    }

    @Override
    public List<MerchantConfig> listSubMerchants(String partnerMchId) {
        return this.list(Wrappers.<MerchantConfig>lambdaQuery()
            .eq(MerchantConfig::getParentMchId, partnerMchId)
            .eq(MerchantConfig::getEnabled, CommonFlag.ENABLED)
            .eq(MerchantConfig::getIsDeleted, CommonFlag.NOT_DELETED));
    }

    @Override
    public MerchantConfig createMerchant(MerchantConfig merchant) {
        // 校验
        if (merchant.getMchId() == null || merchant.getMchId().isEmpty()) {
            throw new IllegalArgumentException("mch_id 不能为空");
        }
        if (merchant.getApiV3Key() == null || merchant.getApiV3Key().length() < 32) {
            throw new IllegalArgumentException("api_v3_key 必须 32 位");
        }
        if (merchant.getCertSerialNo() == null || merchant.getCertPrivateKeyPath() == null) {
            throw new IllegalArgumentException("cert_serial_no + cert_private_key_path 不能为空");
        }

        boolean isPartner = MerchantMode.PARTNER.equals(merchant.getMode());
        if (isPartner) {
            if (merchant.getParentMchId() == null || merchant.getParentMchId().isEmpty()) {
                throw new IllegalArgumentException("PARTNER 模式必须配置 parent_mch_id（服务商号）");
            }
            if (merchant.getSubAppId() == null || merchant.getSubAppId().isEmpty()) {
                throw new IllegalArgumentException("PARTNER 模式必须配置 sub_app_id（特约商户 AppID）");
            }
        } else {
            // DIRECT 模式：清空 PARTNER 字段
            merchant.setParentMchId(null);
            merchant.setSubAppId(null);
        }

        // 唯一性校验
        MerchantConfig existing = getByMchId(merchant.getMchId());
        if (existing != null) {
            throw new IllegalArgumentException("mch_id 已存在：" + merchant.getMchId());
        }

        // 默认值
        if (merchant.getEnabled() == null) {
            merchant.setEnabled(CommonFlag.ENABLED);
        }
        if (merchant.getMode() == null) {
            merchant.setMode(MerchantMode.DIRECT);
        }

        boolean ok = this.save(merchant);
        log.info("[MerchantConfig] 创建商户: mchId={}, mode={}, ok={}",
            merchant.getMchId(), merchant.getMode(), ok);
        return ok ? merchant : null;
    }

    @Override
    public boolean updateMerchant(MerchantConfig merchant) {
        if (merchant.getMchId() == null) {
            throw new IllegalArgumentException("mch_id 不能为空");
        }
        MerchantConfig existing = getByMchId(merchant.getMchId());
        if (existing == null) {
            throw new IllegalArgumentException("商户不存在：" + merchant.getMchId());
        }
        // 不允许改 mch_id（主键）
        boolean ok = this.updateById(merchant);
        log.info("[MerchantConfig] 更新商户: mchId={}, ok={}", merchant.getMchId(), ok);
        return ok;
    }

    @Override
    public boolean deleteMerchant(String mchId) {
        MerchantConfig existing = getByMchId(mchId);
        if (existing == null) {
            return false;
        }
        boolean ok = this.removeById(existing.getId());
        log.info("[MerchantConfig] 删除商户: mchId={}, ok={}", mchId, ok);
        return ok;
    }
}
