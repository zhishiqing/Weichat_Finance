package com.weichat.finance.payment.v3.jsapi;

import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.payment.v3.jsapi.request.JsapiCreateRequest;

/**
 * JSAPI 支付服务接口（项目内部抽象，区别于 SDK 的 JsapiService 类）。
 *
 * <p>Phase 2/3 提供两套实现：</p>
 * <ul>
 *   <li>{@link MockJsapiService}：返回假数据，本地零风险（默认）</li>
 *   <li>{@link RealJsapiService}：调用微信支付 V3 真实接口（生产）</li>
 * </ul>
 *
 * <p>由 {@code wechatpay.mode} 配置切换：MOCK（默认）/ REAL。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
public interface JsapiService {

    /**
     * 创建 JSAPI 支付订单，返回 prepay_id。
     *
     * @param request  下单请求（包含商户订单号、金额、openid 等）
     * @param merchant 商户配置
     * @return 响应（含 prepay_id）
     */
    JsapiCreateResponse create(JsapiCreateRequest request, MerchantConfig merchant);

    /**
     * 按商户订单号查询支付订单。
     *
     * @param outTradeNo 商户订单号
     * @param merchant   商户配置
     * @return 响应（支付状态等）
     */
    JsapiQueryResponse queryByOutTradeNo(String outTradeNo, MerchantConfig merchant);

    /**
     * 按商户订单号关单（未支付订单）。
     *
     * @param outTradeNo 商户订单号
     * @param merchant   商户配置
     */
    void closeByOutTradeNo(String outTradeNo, MerchantConfig merchant);
}
