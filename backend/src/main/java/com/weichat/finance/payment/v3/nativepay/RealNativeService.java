package com.weichat.finance.payment.v3.nativepay;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.enums.MerchantMode;
import com.weichat.finance.payment.client.WechatPayConfigManager;
import com.weichat.finance.payment.config.WechatPayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 真实 Native 支付服务（生产模式）。
 *
 * <p>对应微信支付 V3：{@code POST /v3/pay/transactions/native}</p>
 *
 * @author panhw
 * @since 2026-09-16
 */
@Service
@ConditionalOnProperty(prefix = "wechatpay", name = "mode", havingValue = "REAL")
public class RealNativeService implements NativeService {

    private static final Logger log = LoggerFactory.getLogger(RealNativeService.class);

    @Autowired
    @Qualifier("wechatPayConfig")
    private Config wechatPayConfig;

    @Autowired
    private WechatPayProperties wechatPayProperties;

    @Autowired
    private WechatPayConfigManager configManager;

    private NativePayService sdkService(MerchantConfig merchant) {
        Config cfg = configManager.getConfigForMerchant(merchant);
        return new NativePayService.Builder()
            .config(cfg)
            .build();
    }

    @Override
    public NativeCreateResponse create(NativeCreateRequest request, MerchantConfig merchant) {
        log.info("[Real] 创建 Native 订单: outTradeNo={}, amountTotal={}, mode={}",
            request.getOutTradeNo(), request.getAmountTotal(), merchant.getMode());

        PrepayRequest sdkReq = new PrepayRequest();
        // PARTNER 模式：appid 传 sub_app_id，sub_mch_id 传子商户号
        if (MerchantMode.PARTNER.equals(merchant.getMode())) {
            sdkReq.setAppid(merchant.getSubAppId() != null
                ? merchant.getSubAppId()
                : wechatPayProperties.getMerchant().getAppId());
            try {
                sdkReq.getClass().getMethod("setSubMchid", String.class)
                    .invoke(sdkReq, merchant.getMchId());
            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                log.debug("[Real] SDK 不支持 setSubMchid（Native），回退 setMchid: {}", e.getMessage());
                sdkReq.setMchid(merchant.getMchId());
            }
        } else {
            sdkReq.setAppid(merchant.getAppId() != null
                ? merchant.getAppId()
                : wechatPayProperties.getMerchant().getAppId());
            sdkReq.setMchid(merchant.getMchId());
        }
        sdkReq.setOutTradeNo(request.getOutTradeNo());
        sdkReq.setDescription(request.getDescription());
        sdkReq.setAttach(request.getAttach());
        sdkReq.setNotifyUrl(merchant.getNotifyUrlBase() != null
            ? merchant.getNotifyUrlBase() + "/notify/v3/pay/success"
            : wechatPayProperties.getNotifyUrlBase() + "/notify/v3/pay/success");

        com.wechat.pay.java.service.payments.nativepay.model.Amount amount =
            new com.wechat.pay.java.service.payments.nativepay.model.Amount();
        amount.setTotal(request.getAmountTotal().intValue()); // SDK Integer 兼容
        amount.setCurrency(request.getCurrency() != null ? request.getCurrency() : "CNY");
        sdkReq.setAmount(amount);

        try {
            PrepayResponse prepayResp = sdkService(merchant).prepay(sdkReq);
            log.info("[Real] Native 返回 code_url 长度={}", prepayResp.getCodeUrl() != null ? prepayResp.getCodeUrl().length() : 0);

            NativeCreateResponse response = new NativeCreateResponse();
            response.setCodeUrl(prepayResp.getCodeUrl());
            response.setSource("REAL");
            return response;
        } catch (Exception e) {
            log.error("[Real] 创建 Native 订单失败: outTradeNo={}, error={}",
                request.getOutTradeNo(), e.getMessage(), e);
            throw new RuntimeException("调用微信支付 V3 Native 下单失败: " + e.getMessage(), e);
        }
    }
}
