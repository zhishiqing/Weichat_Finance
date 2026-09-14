package com.weichat.finance.controller;

import com.weichat.finance.common.R;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.PayOrder;
import com.weichat.finance.payment.v3.jsapi.JsapiCreateResponse;
import com.weichat.finance.payment.v3.jsapi.JsapiService;
import com.weichat.finance.payment.v3.jsapi.request.JsapiCreateRequest;
import com.weichat.finance.service.MerchantConfigService;
import com.weichat.finance.service.PayOrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JSAPI 支付接口。
 *
 * <p>对外暴露：</p>
 * <ul>
 *   <li>{@code POST /api/v1/payment/jsapi/create} · 创建 JSAPI 支付订单</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-14
 */
@RestController
@RequestMapping("/v1/payment/jsapi")
public class JsapiController {

    private static final Logger log = LoggerFactory.getLogger(JsapiController.class);

    private final JsapiService jsapiService;
    private final MerchantConfigService merchantConfigService;
    private final PayOrderService payOrderService;

    public JsapiController(JsapiService jsapiService,
                           MerchantConfigService merchantConfigService,
                           PayOrderService payOrderService) {
        this.jsapiService = jsapiService;
        this.merchantConfigService = merchantConfigService;
        this.payOrderService = payOrderService;
    }

    /**
     * 创建 JSAPI 支付订单。
     */
    @PostMapping("/create")
    public R<JsapiCreateResponse> create(@Valid @RequestBody JsapiCreateRequest request) {
        log.info("创建 JSAPI 订单: outTradeNo={}, amountTotal={}",
            request.getOutTradeNo(), request.getAmountTotal());

        PayOrder existing = payOrderService.getByOutTradeNo(request.getOutTradeNo());
        if (existing != null) {
            log.warn("商户订单号已存在: outTradeNo={}, status={}",
                request.getOutTradeNo(), existing.getStatus());
            return R.fail(409, "商户订单号已存在");
        }

        MerchantConfig merchant = merchantConfigService.getByMchId("PLACEHOLDER_MCH_ID");
        if (merchant == null) {
            return R.fail(500, "未配置默认商户");
        }

        PayOrder order = new PayOrder();
        order.setOutTradeNo(request.getOutTradeNo());
        order.setMchId(merchant.getMchId());
        order.setAppId(merchant.getAppId());
        order.setDescription(request.getDescription());
        order.setAmountTotal(request.getAmountTotal());
        order.setCurrency(request.getCurrency() != null ? request.getCurrency() : "CNY");
        order.setOpenid(request.getOpenid());
        order.setProductType("JSAPI");
        order.setStatus("SUBMITTING");
        order.setAttach(request.getAttach());
        order.setNotifyUrl(merchant.getNotifyUrlBase() + "/notify/v3/pay/success");
        payOrderService.save(order);
        log.info("业务订单已落库: id={}, outTradeNo={}", order.getId(), order.getOutTradeNo());

        JsapiCreateResponse response = jsapiService.create(request, merchant);

        order.setStatus("CREATED");
        payOrderService.updateById(order);

        return R.ok(response);
    }
}
