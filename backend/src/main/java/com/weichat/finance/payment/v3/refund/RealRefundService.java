package com.weichat.finance.payment.v3.refund;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.QueryByOutRefundNoRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.PayRefund;
import com.weichat.finance.payment.config.WechatPayProperties;
import com.weichat.finance.payment.v3.refund.request.RefundCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private Config wechatPayConfig;

    @Autowired
    private WechatPayProperties wechatPayProperties;

    private RefundService sdkService() {
        return new RefundService.Builder()
            .config(wechatPayConfig)
            .build();
    }

    @Override
    public RefundCreateResponse create(RefundCreateRequest request, MerchantConfig merchant, PayRefund refundEntity) {
        log.info("[Real] 申请退款: outRefundNo={}, outTradeNo={}, amountRefund={}",
            request.getOutRefundNo(), request.getOutTradeNo(), request.getAmountRefund());

        CreateRequest sdkReq = new CreateRequest();
        sdkReq.setOutTradeNo(request.getOutTradeNo());
        sdkReq.setOutRefundNo(request.getOutRefundNo());
        sdkReq.setReason(request.getReason());
        sdkReq.setNotifyUrl(merchant.getNotifyUrlBase() != null
            ? merchant.getNotifyUrlBase() + "/notify/v3/refund/success"
            : wechatPayProperties.getNotifyUrlBase() + "/notify/v3/refund/success");

        AmountReq amount = new AmountReq();
        amount.setRefund(request.getAmountRefund());
        amount.setTotal(request.getAmountTotal());
        amount.setCurrency("CNY");
        sdkReq.setAmount(amount);

        try {
            Refund refund = sdkService().create(sdkReq);
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
        log.info("[Real] 查询退款单: outRefundNo={}", outRefundNo);
        QueryByOutRefundNoRequest req = new QueryByOutRefundNoRequest();
        req.setOutRefundNo(outRefundNo);

        try {
            Refund refund = sdkService().queryByOutRefundNo(req);
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
