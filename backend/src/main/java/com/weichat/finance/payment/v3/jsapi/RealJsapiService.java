package com.weichat.finance.payment.v3.jsapi;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.CloseOrderRequest;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayResponse;
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.payment.config.WechatPayProperties;
import com.weichat.finance.payment.v3.jsapi.request.JsapiCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 真实 JSAPI 支付服务（生产模式）。
 *
 * <h3>激活条件</h3>
 * {@code wechatpay.mode=REAL} 时激活，调用微信支付 V3 真实接口。
 *
 * <h3>SDK</h3>
 * 使用 wechatpay-java SDK 0.2.17 的 {@code com.wechat.pay.java.service.payments.jsapi.JsapiService}。
 *
 * <h3>实现细节</h3>
 * <ul>
 *   <li>{@link #create}：POST /v3/pay/transactions/jsapi → 返回 prepay_id</li>
 *   <li>{@link #queryByOutTradeNo}：GET /v3/pay/transactions/out-trade-no/{out_trade_no} → 返回 Transaction</li>
 *   <li>{@link #closeByOutTradeNo}：POST /v3/pay/transactions/out-trade-no/{out_trade_no}/close</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-16
 */
@Service
@ConditionalOnProperty(prefix = "wechatpay", name = "mode", havingValue = "REAL")
public class RealJsapiService implements com.weichat.finance.payment.v3.jsapi.JsapiService {

    private static final Logger log = LoggerFactory.getLogger(RealJsapiService.class);

    @Autowired
    private Config wechatPayConfig;

    @Autowired
    private WechatPayProperties wechatPayProperties;

    /** SDK JSAPI Service（每个请求 new 一次，因为底层 HttpClient 不复用） */
    private JsapiService sdkService() {
        return new JsapiService.Builder()
            .config(wechatPayConfig)
            .build();
    }

    @Override
    public JsapiCreateResponse create(JsapiCreateRequest request, MerchantConfig merchant) {
        log.info("[Real] 创建 JSAPI 订单: outTradeNo={}, amountTotal={}, openid={}",
            request.getOutTradeNo(), request.getAmountTotal(), request.getOpenid());

        PrepayRequest sdkReq = new PrepayRequest();
        sdkReq.setAppid(merchant.getAppId() != null ? merchant.getAppId() : wechatPayProperties.getMerchant().getAppId());
        sdkReq.setMchid(merchant.getMchId());
        sdkReq.setOutTradeNo(request.getOutTradeNo());
        sdkReq.setDescription(request.getDescription());
        sdkReq.setAttach(request.getAttach());
        sdkReq.setNotifyUrl(merchant.getNotifyUrlBase() != null
            ? merchant.getNotifyUrlBase() + "/notify/v3/pay/success"
            : wechatPayProperties.getNotifyUrlBase() + "/notify/v3/pay/success");
        if (request.getTimeExpire() != null) {
            sdkReq.setTimeExpire(request.getTimeExpire());
        }

        Amount amount = new Amount();
        // SDK Amount.total 是 Integer（微信上限 5000万，完全够用）
        amount.setTotal(request.getAmountTotal().intValue());
        amount.setCurrency(request.getCurrency() != null ? request.getCurrency() : "CNY");
        sdkReq.setAmount(amount);

        Payer payer = new Payer();
        payer.setOpenid(request.getOpenid());
        sdkReq.setPayer(payer);

        try {
            PrepayResponse prepayResp = sdkService().prepay(sdkReq);
            log.info("[Real] 微信返回 prepay_id={}", prepayResp.getPrepayId());

            JsapiCreateResponse response = new JsapiCreateResponse();
            response.setPrepayId(prepayResp.getPrepayId());
            response.setSource("REAL");
            return response;
        } catch (Exception e) {
            log.error("[Real] 创建 JSAPI 订单失败: outTradeNo={}, error={}",
                request.getOutTradeNo(), e.getMessage(), e);
            throw new RuntimeException("调用微信支付 V3 JSAPI 下单失败: " + e.getMessage(), e);
        }
    }

    @Override
    public JsapiQueryResponse queryByOutTradeNo(String outTradeNo, MerchantConfig merchant) {
        log.info("[Real] 查询订单: outTradeNo={}", outTradeNo);
        QueryOrderByOutTradeNoRequest req = new QueryOrderByOutTradeNoRequest();
        req.setMchid(merchant.getMchId());
        req.setOutTradeNo(outTradeNo);

        try {
            Transaction tx = sdkService().queryOrderByOutTradeNo(req);
            return convert(tx, outTradeNo);
        } catch (Exception e) {
            log.error("[Real] 查询订单失败: outTradeNo={}, error={}", outTradeNo, e.getMessage(), e);
            throw new RuntimeException("调用微信支付 V3 查单失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void closeByOutTradeNo(String outTradeNo, MerchantConfig merchant) {
        log.info("[Real] 关单: outTradeNo={}", outTradeNo);
        CloseOrderRequest req = new CloseOrderRequest();
        req.setMchid(merchant.getMchId());
        req.setOutTradeNo(outTradeNo);
        try {
            sdkService().closeOrder(req);
            log.info("[Real] 关单成功: outTradeNo={}", outTradeNo);
        } catch (Exception e) {
            log.error("[Real] 关单失败: outTradeNo={}, error={}", outTradeNo, e.getMessage(), e);
            throw new RuntimeException("调用微信支付 V3 关单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 转换 SDK Transaction → 项目响应 DTO。
     */
    private JsapiQueryResponse convert(Transaction tx, String outTradeNo) {
        JsapiQueryResponse response = new JsapiQueryResponse();
        response.setOutTradeNo(outTradeNo);
        response.setTransactionId(tx.getTransactionId());
        if (tx.getTradeState() != null) {
            // SDK TradeState 枚举值与微信 PayStatus 字符串基本一致
            response.setPayStatus(tx.getTradeState().name());
        }
        if (tx.getAmount() != null && tx.getAmount().getPayerTotal() != null) {
            // SDK TransactionAmount.payerTotal 是 Integer，转 Long 兼容业务层
            response.setAmountPayerTotal(tx.getAmount().getPayerTotal().longValue());
        }
        response.setBankType(tx.getBankType());
        if (tx.getSuccessTime() != null && !tx.getSuccessTime().isEmpty()) {
            try {
                OffsetDateTime t = OffsetDateTime.parse(tx.getSuccessTime(),
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                response.setSuccessTime(t.toLocalDateTime());
            } catch (Exception ignored) {
                // 解析失败时忽略
            }
        }
        response.setSource("REAL");
        return response;
    }
}
