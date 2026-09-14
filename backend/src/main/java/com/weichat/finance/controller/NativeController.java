package com.weichat.finance.controller;

import com.weichat.finance.common.R;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.PayOrder;
import com.weichat.finance.payment.v3.nativepay.NativeCreateRequest;
import com.weichat.finance.payment.v3.nativepay.NativeCreateResponse;
import com.weichat.finance.payment.v3.nativepay.NativeService;
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
 * Native 支付接口。
 *
 * <p>暴露：{@code POST /api/v1/payment/native/create} · 创建 Native 支付订单</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@RestController
@RequestMapping("/v1/payment")
public class NativeController {

    private static final Logger log = LoggerFactory.getLogger(NativeController.class);
    private static final String DEFAULT_MCH_ID = "PLACEHOLDER_MCH_ID";

    private final NativeService nativeService;
    private final MerchantConfigService merchantConfigService;
    private final PayOrderService payOrderService;

    public NativeController(NativeService nativeService,
                            MerchantConfigService merchantConfigService,
                            PayOrderService payOrderService) {
        this.nativeService = nativeService;
        this.merchantConfigService = merchantConfigService;
        this.payOrderService = payOrderService;
    }

    /**
     * 创建 Native 支付订单。
     *
     * <p>Phase 2.x 简化设计：金额从前端入参。</p>
     *
     * <p>🟠 TODO Phase 4：引入 {@code t_business_order}，前端只传 {@code business_order_id}，金额后端查。</p>
     */
    @PostMapping("/native/create")
    public R<NativeCreateResponse> create(@Valid @RequestBody NativeCreateRequest request) {
        log.info("创建 Native 订单: outTradeNo={}, amountTotal={}",
            request.getOutTradeNo(), request.getAmountTotal());

        PayOrder existing = payOrderService.getByOutTradeNo(request.getOutTradeNo());
        if (existing != null) {
            log.warn("商户订单号已存在: outTradeNo={}", request.getOutTradeNo());
            return R.fail(409, "商户订单号已存在");
        }

        MerchantConfig merchant = merchantConfigService.getByMchId(DEFAULT_MCH_ID);
        if (merchant == null) {
            return R.fail(500, "未配置默认商户");
        }

        // 落业务订单
        PayOrder order = new PayOrder();
        order.setOutTradeNo(request.getOutTradeNo());
        order.setMchId(merchant.getMchId());
        order.setAppId(merchant.getAppId());
        order.setDescription(request.getDescription());
        order.setAmountTotal(request.getAmountTotal());
        order.setCurrency(request.getCurrency() != null ? request.getCurrency() : "CNY");
        order.setProductType("NATIVE");
        order.setStatus("SUBMITTING");
        order.setAttach(request.getAttach());
        order.setNotifyUrl(merchant.getNotifyUrlBase() + "/notify/v3/pay/success");
        payOrderService.save(order);
        log.info("业务订单已落库: id={}, outTradeNo={}", order.getId(), order.getOutTradeNo());

        NativeCreateResponse response = nativeService.create(request, merchant);

        order.setStatus("CREATED");
        payOrderService.updateById(order);
        return R.ok(response);
    }
}
