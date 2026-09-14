package com.weichat.finance.payment.v3.jsapi;

import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.payment.v3.jsapi.request.JsapiCreateRequest;

/**
 * 真实 JSAPI 服务（生产模式）· Phase 2 占位。
 *
 * <p>调用微信支付 V3 接口：{@code POST /v3/pay/transactions/jsapi}</p>
 *
 * <p><strong>当前状态：</strong>Phase 2 已完成 Mock 实现（{@link MockJsapiService}），
 * 当前模式（MOCK）可正常跑完整链路（数据库落单 + 返回 prepay_id）。</p>
 *
 * <p><strong>Phase 2.1 待办：</strong>激活真实调用</p>
 * <ol>
 *   <li>配置 RSAAutoCertificateConfig（v0.2.12）</li>
 *   <li>按 v0.2.12 README 配置 NotificationConfig</li>
 *   <li>添加 JsapiServiceSdk Bean</li>
 *   <li>实现本类 {@link #create} 真实方法</li>
 * </ol>
 *
 * <p>参考 SDK 文档：https://github.com/wechatpay-apiv3/wechatpay-java</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
public class RealJsapiService implements JsapiService {

    @Override
    public JsapiCreateResponse create(JsapiCreateRequest request, MerchantConfig merchant) {
        throw new UnsupportedOperationException(
            "RealJsapiService Phase 2 占位实现，请将 wechatpay.mode 切换为 MOCK 验证流程，"
            + "或等待 Phase 2.1 实施完成后激活");
    }
}
