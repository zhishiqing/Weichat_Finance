package com.weichat.finance.controller;

import com.weichat.finance.common.R;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.PayOrder;
import com.weichat.finance.entity.enums.OrderStatus;
import com.weichat.finance.entity.enums.ProductType;
import com.weichat.finance.payment.v3.jsapi.JsapiCreateResponse;
import com.weichat.finance.payment.v3.jsapi.JsapiQueryResponse;
import com.weichat.finance.payment.v3.jsapi.JsapiService;
import com.weichat.finance.payment.v3.jsapi.request.JsapiCreateRequest;
import com.weichat.finance.service.MerchantConfigService;
import com.weichat.finance.service.PayOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 *   <li>{@code POST /v1/payment/jsapi/create} · 创建 JSAPI 支付订单</li>
 *   <li>{@code GET  /v1/payment/order/{outTradeNo}} · 查询支付订单</li>
 *   <li>{@code POST /v1/payment/order/{outTradeNo}/close} · 关单</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-14
 */
@RestController
@RequestMapping("/v1/payment")
@Tag(name = "JSAPI 支付", description = "JSAPI 统一下单 / 查单 / 关单")
public class JsapiController {

    private static final Logger log = LoggerFactory.getLogger(JsapiController.class);
    private static final String DEFAULT_MCH_ID = "1674723182";

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
     * <p>TODO Phase 4：引入 {@code t_business_order}，前端只传 {@code business_order_id}，
     * 金额/描述/商户号/用户标识 全部从后端业务订单读取，杜绝前端篡改。</p>
     */
    @Operation(summary = "创建 JSAPI 支付订单", description = "传入商户订单号、金额、openid，返回 prepay_id 用于前端调起微信支付")
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
        order.setProductType(ProductType.JSAPI);
        order.setStatus(OrderStatus.SUBMITTING);
        order.setAttach(request.getAttach());
        order.setNotifyUrl(merchant.getNotifyUrlBase() + "/notify/v3/pay/success");
        payOrderService.save(order);
        log.info("业务订单已落库: id={}, outTradeNo={}", order.getId(), order.getOutTradeNo());

        JsapiCreateResponse response = jsapiService.create(request, merchant);

        order.setStatus(OrderStatus.CREATED);
        payOrderService.updateById(order);

        // v1.4 主动轮询：下单成功后立即入队（next_query_at = NOW()），PayOrderPollScheduler 会扫描查单
        // 仅 REAL 模式需要轮询；MOCK 模式下 next_query_at 无意义但不会触发真实查询
        if (response.getPrepayId() != null && !"MOCK".equalsIgnoreCase(response.getSource())) {
            payOrderService.enqueueQuery(order.getId());
            log.info("✅ 订单已入轮询队列: outTradeNo={}, orderId={}",
                order.getOutTradeNo(), order.getId());
        }

        return R.ok(response);
    }

    /**
     * 查询支付订单（按商户订单号）。
     */
    @Operation(summary = "查询支付订单", description = "按商户订单号查询支付状态，Mock 模式返回 NOTPAY（未支付）")
    @GetMapping("/order/{outTradeNo}")
    public R<JsapiQueryResponse> queryByOutTradeNo(@Parameter(description = "商户订单号") @PathVariable String outTradeNo) {
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
    @Operation(summary = "关单", description = "关闭未支付订单（Mock 仅日志记录），更新订单状态为 CLOSED")
    @PostMapping("/order/{outTradeNo}/close")
    public R<Void> closeByOutTradeNo(@Parameter(description = "商户订单号") @PathVariable String outTradeNo) {
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
        order.setStatus(OrderStatus.CLOSED);
        payOrderService.updateById(order);
        log.info("订单状态已更新为 CLOSED: outTradeNo={}", outTradeNo);
        return R.ok();
    }
}
