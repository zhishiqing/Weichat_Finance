package com.weichat.finance.payment.v3.refund;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.QueryByOutRefundNoRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.PayRefund;
import com.weichat.finance.entity.enums.MerchantMode;
import com.weichat.finance.payment.client.WechatPayConfigManager;
import com.weichat.finance.payment.config.WechatPayProperties;
import com.weichat.finance.payment.v3.refund.request.RefundCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 真实退款服务（生产模式）。
 *
 * <p>对应微信支付 V3：{@code POST /v3/refund/domestic/refunds}</p>
 *
 * @author panhw
 * @since 2026-09-16
 */
@Service
@ConditionalOnProperty(prefix = "wechatpay", name = "mode", havingValue = "REAL")
public class RealRefundService implements com.weichat.finance.payment.v3.refund.RefundService {

    private static final Logger log = LoggerFactory.getLogger(RealRefundService.class);

    @Autowired
    @Qualifier("wechatPayConfig")
    private Config wechatPayConfig;

    @Autowired
    private WechatPayProperties wechatPayProperties;

    @Autowired
    private WechatPayConfigManager configManager;

    private RefundService sdkService(MerchantConfig merchant) {
        Config cfg = configManager.getConfigForMerchant(merchant);
        return new RefundService.Builder()
            .config(cfg)
            .build();
    }

    @Override
    public RefundCreateResponse create(RefundCreateRequest request, MerchantConfig merchant, PayRefund refundEntity) {
        log.info("[Real] 申请退款: outRefundNo={}, outTradeNo={}, amountRefund={}, mode={}",
            request.getOutRefundNo(), request.getOutTradeNo(), request.getAmountRefund(), merchant.getMode());

        CreateRequest sdkReq = new CreateRequest();
        sdkReq.setOutTradeNo(request.getOutTradeNo());
        sdkReq.setOutRefundNo(request.getOutRefundNo());
        sdkReq.setReason(request.getReason());
        sdkReq.setNotifyUrl(merchant.getNotifyUrlBase() != null
            ? merchant.getNotifyUrlBase() + "/notify/v3/refund/success"
            : wechatPayProperties.getNotifyUrlBase() + "/notify/v3/refund/success");

        // PARTNER 模式：传 sub_mch_id
        if (MerchantMode.PARTNER.equals(merchant.getMode())) {
            try {
                sdkReq.getClass().getMethod("setSubMchid", String.class)
                    .invoke(sdkReq, merchant.getMchId());
            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                log.debug("[Real] SDK 不支持 setSubMchid（退款创建），忽略: {}", e.getMessage());
            }
        }

        AmountReq amount = new AmountReq();
        amount.setRefund(request.getAmountRefund());
        amount.setTotal(request.getAmountTotal());
        amount.setCurrency("CNY");
        sdkReq.setAmount(amount);

        try {
            Refund refund = sdkService(merchant).create(sdkReq);
            log.info("[Real] 微信受理退款: refundId={}, status={}", refund.getRefundId(), refund.getStatus());

            RefundCreateResponse response = new RefundCreateResponse();
            response.setOutRefundNo(request.getOutRefundNo());
            response.setRefundId(refund.getRefundId());
            // SDK Status 是枚举（PROCESSING/SUCCESS/CLOSED/ABNORMAL），name() 即字符串
            response.setRefundStatus(refund.getStatus() != null ? refund.getStatus().name() : "PROCESSING");
            response.setSource("REAL");
            return response;
        } catch (Exception e) {
            log.error("[Real] 申请退款失败: outRefundNo={}, error={}", request.getOutRefundNo(), e.getMessage(), e);
            throw new RuntimeException("调用微信支付 V3 退款失败: " + e.getMessage(), e);
        }
    }

    @Override
    public RefundCreateResponse query(String outRefundNo, MerchantConfig merchant) {
        log.info("[Real] 查询退款单: outRefundNo={}, mode={}", outRefundNo, merchant.getMode());
        QueryByOutRefundNoRequest req = new QueryByOutRefundNoRequest();
        req.setOutRefundNo(outRefundNo);

        // PARTNER 模式：传 sub_mch_id
        if (MerchantMode.PARTNER.equals(merchant.getMode())) {
            try {
                req.getClass().getMethod("setSubMchid", String.class)
                    .invoke(req, merchant.getMchId());
            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                log.debug("[Real] SDK 不支持 setSubMchid（退款查询），忽略: {}", e.getMessage());
            }
        }

        try {
            Refund refund = sdkService(merchant).queryByOutRefundNo(req);
            RefundCreateResponse response = new RefundCreateResponse();
            response.setOutRefundNo(outRefundNo);
            response.setRefundId(refund.getRefundId());
            response.setRefundStatus(refund.getStatus() != null ? refund.getStatus().name() : "PROCESSING");
            response.setSource("REAL");
            return response;
        } catch (Exception e) {
            log.error("[Real] 查询退款单失败: outRefundNo={}, error={}", outRefundNo, e.getMessage(), e);
            throw new RuntimeException("调用微信支付 V3 退款查询失败: " + e.getMessage(), e);
        }
    }
}
