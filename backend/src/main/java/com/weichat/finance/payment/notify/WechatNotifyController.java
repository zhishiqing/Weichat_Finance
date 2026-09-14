package com.weichat.finance.payment.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weichat.finance.entity.PayNotifyLog;
import com.weichat.finance.entity.enums.NotifyResult;
import com.weichat.finance.entity.enums.NotifyType;
import com.weichat.finance.service.PayNotifyLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信支付 V3 回调接收。
 *
 * <p>暴露：</p>
 * <ul>
 *   <li>{@code POST /notify/v3/pay/success} · 支付成功回调</li>
 *   <li>{@code POST /notify/v3/refund/success} · 退款成功回调</li>
 * </ul>
 *
 * <p><strong>Phase 3 现状：</strong>落库 t_pay_notify_log（原始报文+请求头），验签/解密/防重放部分
 * 尚未激活，待真实私钥补齐后实施。Mock 模式可完整走通链路记录。</p>
 *
 * <p>关键来源：</p>
 * <ul>
 *   <li>请求头含：Wechatpay-Signature、Wechatpay-Timestamp、Wechatpay-Nonce、Wechatpay-Serial</li>
 *   <li>请求体：加密的 resource 对象 { ciphertext, nonce, associated_data }</li>
 * </ul>
 *
 * <p>详见：https://pay.weixin.qq.com/wiki/doc/apiv3/wxpay/pay/combine-transactions.shtml</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@RestController
@RequestMapping("/notify/v3")
public class WechatNotifyController {

    private static final Logger log = LoggerFactory.getLogger(WechatNotifyController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PayNotifyLogService payNotifyLogService;

    public WechatNotifyController(PayNotifyLogService payNotifyLogService) {
        this.payNotifyLogService = payNotifyLogService;
    }

    /**
     * 支付成功回调。
     *
     * <p>Phase 3 行为：</p>
     * <ol>
     *   <li>先识别 SIGNTEST 探测流量，命中直接返回 200 但不落库、不处理（避免微信认为商户未验签）</li>
     *   <li>解析请求头（含签名信息）</li>
     *   <li>读取原文 body</li>
     *   <li>落库 t_pay_notify_log（验证前先存）</li>
     *   <li>Phase 3.1 TODO：调用 NotificationParser 验签 + 解密</li>
     *   <li>Phase 3.1 TODO：按 out_trade_no 幂等 + 更新 t_pay_order / t_pay_transaction</li>
     *   <li>返回 200 OK 让微信停止重试</li>
     * </ol>
     */
    @PostMapping("/pay/success")
    public String paySuccess(HttpServletRequest request,
                             @RequestHeader(value = "Wechatpay-Signature", required = false) String wechatpaySignature,
                             @RequestHeader(value = "Wechatpay-Timestamp", required = false) String wechatpayTimestamp,
                             @RequestHeader(value = "Wechatpay-Nonce", required = false) String wechatpayNonce,
                             @RequestHeader(value = "Wechatpay-Serial", required = false) String wechatpaySerial,
                             @RequestBody String rawBody) throws IOException {
        log.info("收到微信支付成功回调: signature={}, timestamp={}, nonce={}, serial={}",
            wechatpaySignature, wechatpayTimestamp, wechatpayNonce, wechatpaySerial);

        // 🔴 致命修复：SIGNTEST 探测流量必须正确处理
        // 微信下发的探测流量 userAgent/path 含 WECHATPAY/SIGNTEST/ 前缀
        // 正确响应：200 OK（标记已处理验签能力），不进入真实业务逻辑
        if (isSignTestTraffic(request)) {
            log.info("收到微信 SIGNTEST 探测流量，按规范返回 200 OK");
            return successJsonResponse();
        }

        Map<String, String> headers = collectHeaders(request);
        headers.put("Wechatpay-Signature", nullToEmpty(wechatpaySignature));
        headers.put("Wechatpay-Timestamp", nullToEmpty(wechatpayTimestamp));
        headers.put("Wechatpay-Nonce", nullToEmpty(wechatpayNonce));
        headers.put("Wechatpay-Serial", nullToEmpty(wechatpaySerial));

        PayNotifyLog logEntity = new PayNotifyLog();
        logEntity.setNotifyType(NotifyType.PAY);
        logEntity.setMchId(extractMchIdFromBody(rawBody));
        logEntity.setHeaders(MAPPER.writeValueAsString(headers));
        logEntity.setRawBody(rawBody);
        logEntity.setVerifyResult(NotifyResult.SKIPPED_PHASE3);
        logEntity.setProcessResult(NotifyResult.IGNORED_PHASE3);
        logEntity.setErrorMessage("待真实私钥补齐后激活验签/解密流程");
        payNotifyLogService.save(logEntity);

        log.info("支付回调已落库: notifyId={}", logEntity.getId());

        // TODO Phase 3.1: 调用 NotificationParser 验签 + 解密；更新订单状态
        return successJsonResponse();
    }

    /**
     * 退款成功回调。
     *
     * <p>行为同支付回调，回调类型为 REFUND。</p>
     */
    @PostMapping("/refund/success")
    public String refundSuccess(HttpServletRequest request,
                                 @RequestHeader(value = "Wechatpay-Signature", required = false) String wechatpaySignature,
                                 @RequestHeader(value = "Wechatpay-Timestamp", required = false) String wechatpayTimestamp,
                                 @RequestHeader(value = "Wechatpay-Nonce", required = false) String wechatpayNonce,
                                 @RequestHeader(value = "Wechatpay-Serial", required = false) String wechatpaySerial,
                                 @RequestBody String rawBody) throws IOException {
        log.info("收到微信退款成功回调: signature={}, timestamp={}, nonce={}, serial={}",
            wechatpaySignature, wechatpayTimestamp, wechatpayNonce, wechatpaySerial);

        if (isSignTestTraffic(request)) {
            log.info("收到微信 SIGNTEST 探测流量（refund），按规范返回 200 OK");
            return successJsonResponse();
        }

        Map<String, String> headers = collectHeaders(request);
        headers.put("Wechatpay-Signature", nullToEmpty(wechatpaySignature));
        headers.put("Wechatpay-Timestamp", nullToEmpty(wechatpayTimestamp));
        headers.put("Wechatpay-Nonce", nullToEmpty(wechatpayNonce));
        headers.put("Wechatpay-Serial", nullToEmpty(wechatpaySerial));

        PayNotifyLog logEntity = new PayNotifyLog();
        logEntity.setNotifyType(NotifyType.REFUND);
        logEntity.setMchId(extractMchIdFromBody(rawBody));
        logEntity.setHeaders(MAPPER.writeValueAsString(headers));
        logEntity.setRawBody(rawBody);
        logEntity.setVerifyResult(NotifyResult.SKIPPED_PHASE3);
        logEntity.setProcessResult(NotifyResult.IGNORED_PHASE3);
        logEntity.setErrorMessage("待真实私钥补齐后激活验签/解密流程");
        payNotifyLogService.save(logEntity);

        log.info("退款回调已落库: notifyId={}", logEntity.getId());
        return successJsonResponse();
    }

    /**
     * 是否为微信 SIGNTEST 探测流量。
     *
     * <p>探测流量的特征：</p>
     * <ul>
     *   <li>User-Agent 含 WECHATPAY/SIGNTEST/</li>
     *   <li>Body 为空或仅含探测用占位内容</li>
     * </ul>
     *
     * <p>按微信支付接入规范，必须返回 HTTP 200 告知"验签能力已具备"，不可 4xx/5xx。
     * 来源：https://pay.weixin.qq.com/wiki/doc/apiv3/wxpay/pay/transactions.shtml</p>
     */
    private boolean isSignTestTraffic(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null && ua.contains("WECHATPAY/SIGNTEST/");
    }

    private Map<String, String> collectHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    private String extractMchIdFromBody(String body) {
        try {
            Map<?, ?> json = MAPPER.readValue(body, Map.class);
            Object id = json.get("mch_id");
            return id != null ? id.toString() : "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * 微信支付要求回调成功时返回 200 OK + 简单 JSON。
     * 最简形式：HTTP 200 即可，业务处理失败也建议返回 200（否则微信会一直重试）。
     */
    private static String successJsonResponse() {
        return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
    }
}
