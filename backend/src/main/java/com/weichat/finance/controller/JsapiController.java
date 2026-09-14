package com.weichat.finance.controller;

import com.weichat.finance.common.R;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.PayOrder;
import com.weichat.finance.payment.v3.jsapi.JsapiCreateResponse;
import com.weichat.finance.payment.v3.jsapi.JsapiQueryResponse;
import com.weichat.finance.payment.v3.jsapi.JsapiService;
import com.weichat.finance.payment.v3.jsapi.request.JsapiCreateRequest;
import com.weichat.finance.service.MerchantConfigService;
import com.weichat.finance.service.PayOrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 *   <li>{@code GET  /api/v1/payment/order/{outTradeNo}} · 查询支付订单</li>
 *   <li>{@code POST /api/v1/payment/order/{outTradeNo}/close} · 关单</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-14
 */
@RestController
@RequestMapping("/v1/payment")
public class JsapiController {

    private static final Logger log = LoggerFactory.getLogger(JsapiController.class);
    private static final String DEFAULT_MCH_ID = "PLACEHOLDER_MCH_ID";

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
     *
     * <p>Phase 2.x 简化设计：业务订单与支付订单合一（{@code t_pay_order}），金额从前端入参（必填校验）。</p>
     *
     * <p>🟠 TODO Phase 4：引入 {@code t_business_order}，前端只传 {@code business_order_id}，
     * 金额/描述/商户号/用户标识 全部从后端业务订单读取，杜绝前端篡改。</p>
     */
    @PostMapping("/jsapi/create")
    public R<JsapiCreateResponse> create(@Valid @RequestBody JsapiCreateRequest request) {
        log.info("创建 JSAPI 订单: outTradeNo={}, amountTotal={}",
            request.getOutTradeNo(), request.getAmountTotal());

        PayOrder existing = payOrderService.getByOutTradeNo(request.getOutTradeNo());
        if (existing != null) {
            log.warn("商户订单号已存在: outTradeNo={}, status={}",
                request.getOutTradeNo(), existing.getStatus());
            return R.fail(409, "商户订单号已存在");
        }

        MerchantConfig merchant = merchantConfigService.getByMchId(DEFAULT_MCH_ID);
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

    /**
     * 查询支付订单（按商户订单号）。
     */
    @GetMapping("/order/{outTradeNo}")
    public R<JsapiQueryResponse> queryByOutTradeNo(@PathVariable String outTradeNo) {
        log.info("查询支付订单: outTradeNo={}", outTradeNo);

        PayOrder order = payOrderService.getByOutTradeNo(outTradeNo);
        if (order == null) {
            return R.fail(404, "商户订单号不存在");
        }

        MerchantConfig merchant = merchantConfigService.getByMchId(order.getMchId());
        if (merchant == null) {
            return R.fail(500, "商户配置缺失");
        }

        JsapiQueryResponse response = jsapiService.queryByOutTradeNo(outTradeNo, merchant);
        return R.ok(response);
    }

    /**
     * 关单（按商户订单号）。
     */
    @PostMapping("/order/{outTradeNo}/close")
    public R<Void> closeByOutTradeNo(@PathVariable String outTradeNo) {
        log.info("关单: outTradeNo={}", outTradeNo);

        PayOrder order = payOrderService.getByOutTradeNo(outTradeNo);
        if (order == null) {
            return R.fail(404, "商户订单号不存在");
        }

        MerchantConfig merchant = merchantConfigService.getByMchId(order.getMchId());
        if (merchant == null) {
            return R.fail(500, "商户配置缺失");
        }

        jsapiService.closeByOutTradeNo(outTradeNo, merchant);

        // 更新订单状态
        order.setStatus("CLOSED");
        payOrderService.updateById(order);
        log.info("订单状态已更新为 CLOSED: outTradeNo={}", outTradeNo);
        return R.ok();
    }
}
