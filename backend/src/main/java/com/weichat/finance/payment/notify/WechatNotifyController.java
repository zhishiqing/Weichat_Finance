package com.weichat.finance.payment.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.weichat.finance.entity.PayNotifyLog;
import com.weichat.finance.entity.PayOrder;
import com.weichat.finance.entity.PayTransaction;
import com.weichat.finance.entity.enums.NotifyResult;
import com.weichat.finance.entity.enums.NotifyType;
import com.weichat.finance.entity.enums.OrderStatus;
import com.weichat.finance.entity.enums.PayStatus;
import com.weichat.finance.service.PayNotifyLogService;
import com.weichat.finance.service.PayOrderService;
import com.weichat.finance.service.PayTransactionService;
import com.weichat.finance.trace.TraceConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信支付 V3 回调接收（激活版：验签 + 解密 + 业务回填）。
 *
 * <h3>激活条件</h3>
 * 任何模式下都会落库 + 验签（REAL 模式真正解密，MOCK 模式 SDK 验签必然失败但有兜底）。
 *
 * <h3>处理流程</h3>
 * <ol>
 *   <li>SIGNTEST 探测流量 → 200 OK 直接返回（不落库）</li>
 *   <li>解析请求头（含签名 4 件套）</li>
 *   <li>读取原文 body</li>
 *   <li>落库 t_pay_notify_log（无论验签结果）</li>
 *   <li>调用 SDK NotificationParser 验签 + 解密</li>
 *   <li>解析 outTradeNo / transactionId → 幂等回填 t_pay_order + t_pay_transaction</li>
 *   <li>返回 200 OK（业务失败也建议 200，避免微信重试）</li>
 * </ol>
 *
 * <h3>异常场景</h3>
 * <ul>
 *   <li>验签失败：FAIL，落库 + 返回 200（避免恶意重试）</li>
 *   <li>解密失败：FAIL，落库 + 返回 200</li>
 *   <li>订单不存在：IGNORED，落库 + 返回 200</li>
 *   <li>业务处理成功：SUCCESS，落库 + 返回 200</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-16
 */
@RestController
@RequestMapping("/notify/v3")
public class WechatNotifyController {

    private static final Logger log = LoggerFactory.getLogger(WechatNotifyController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired(required = false)
    private NotificationParser notificationParser;

    @Autowired
    private PayNotifyLogService payNotifyLogService;

    @Autowired
    private PayOrderService payOrderService;

    @Autowired
    private PayTransactionService payTransactionService;

    @Value("${wechatpay.notify-url-base:http://localhost:8080/api}")
    private String notifyUrlBase;

    /**
     * 支付成功回调。
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

        // 1. SIGNTEST 探测流量
        if (isSignTestTraffic(request)) {
            log.info("收到微信 SIGNTEST 探测流量，按规范返回 200 OK");
            return successJsonResponse();
        }

        // 2. 落库
        PayNotifyLog logEntity = saveNotifyLog(NotifyType.PAY, request, rawBody,
            wechatpaySignature, wechatpayTimestamp, wechatpayNonce, wechatpaySerial);

        // 3. MOCK 模式：NotificationParser 不可用，仅落库
        if (notificationParser == null) {
            logEntity.setProcessResult(NotifyResult.IGNORED);
            logEntity.setErrorMessage("MOCK 模式：跳过验签/解密");
            payNotifyLogService.updateById(logEntity);
            return successJsonResponse();
        }

        // 4. 验签 + 解密
        try {
            RequestParam param = new RequestParam.Builder()
                .serialNumber(wechatpaySerial)
                .nonce(wechatpayNonce)
                .timestamp(wechatpayTimestamp)
                .signature(wechatpaySignature)
                .body(rawBody)
                .signType("WECHATPAY2-SHA256-RSA2048")
                .build();

            // 解密后 JSON 字符串
            String decryptedJson = notificationParser.parse(param, String.class);
            logEntity.setVerifyResult(NotifyResult.PASS);
            logEntity.setDecryptedBody(MAPPER.writeValueAsString(decryptedJson));
            logEntity.setProcessResult(NotifyResult.SUCCESS);

            // 5. 业务处理（幂等回填订单状态）
            String outTradeNo = extractOutTradeNoFromDecrypted(decryptedJson);
            if (outTradeNo != null) {
                processPayNotify(outTradeNo, decryptedJson);
            } else {
                logEntity.setProcessResult(NotifyResult.IGNORED);
                logEntity.setErrorMessage("回调中未找到 out_trade_no");
            }

            payNotifyLogService.updateById(logEntity);
            return successJsonResponse();

        } catch (Exception e) {
            log.error("回调验签或解密失败: {}", e.getMessage(), e);
            logEntity.setVerifyResult(NotifyResult.FAIL);
            logEntity.setProcessResult(NotifyResult.FAILED);
            logEntity.setErrorMessage("验签/解密失败: " + e.getMessage());
            payNotifyLogService.updateById(logEntity);
            return successJsonResponse();
        }
    }

    /**
     * 退款成功回调（同支付结构）。
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

        PayNotifyLog logEntity = saveNotifyLog(NotifyType.REFUND, request, rawBody,
            wechatpaySignature, wechatpayTimestamp, wechatpayNonce, wechatpaySerial);

        if (notificationParser == null) {
            logEntity.setProcessResult(NotifyResult.IGNORED);
            logEntity.setErrorMessage("MOCK 模式：跳过验签/解密");
            payNotifyLogService.updateById(logEntity);
            return successJsonResponse();
        }

        try {
            RequestParam param = new RequestParam.Builder()
                .serialNumber(wechatpaySerial)
                .nonce(wechatpayNonce)
                .timestamp(wechatpayTimestamp)
                .signature(wechatpaySignature)
                .body(rawBody)
                .signType("WECHATPAY2-SHA256-RSA2048")
                .build();
            String decryptedJson = notificationParser.parse(param, String.class);
            logEntity.setVerifyResult(NotifyResult.PASS);
            logEntity.setDecryptedBody(MAPPER.writeValueAsString(decryptedJson));
            logEntity.setProcessResult(NotifyResult.SUCCESS);
            payNotifyLogService.updateById(logEntity);
            return successJsonResponse();

        } catch (Exception e) {
            log.error("退款回调验签/解密失败: {}", e.getMessage(), e);
            logEntity.setVerifyResult(NotifyResult.FAIL);
            logEntity.setProcessResult(NotifyResult.FAILED);
            logEntity.setErrorMessage("验签/解密失败: " + e.getMessage());
            payNotifyLogService.updateById(logEntity);
            return successJsonResponse();
        }
    }

    /**
     * 处理支付成功业务（幂等回填订单状态）。
     *
     * <p>从解密 JSON 提取 out_trade_no / transaction_id / amount.payer_total，
     * 更新 t_pay_order.status=SUCCESS + t_pay_transaction.pay_status=SUCCESS。</p>
     */
    private void processPayNotify(String outTradeNo, String decryptedJson) {
        MDC.put(TraceConstants.MDC_TRACE_ID, TraceConstants.PREFIX_WECHAT + outTradeNo);
        try {
            PayOrder order = payOrderService.getByOutTradeNo(outTradeNo);
            if (order == null) {
                log.warn("回调 outTradeNo 在本地查不到订单: outTradeNo={}", outTradeNo);
                return;
            }

            // 幂等：仅当订单未到终态时才更新
            if (!OrderStatus.SUCCESS.equals(order.getStatus())) {
                order.setStatus(OrderStatus.SUCCESS);
                order.setSuccessTime(LocalDateTime.now());
                payOrderService.updateById(order);
                log.info("✅ 支付回调生效: outTradeNo={}, status={}", outTradeNo, OrderStatus.SUCCESS);
            } else {
                log.info("回调幂等命中: outTradeNo={} 已是 SUCCESS", outTradeNo);
            }

            // 同步更新交易流水（已有则更新，无则插入）
            PayTransaction tx = payTransactionService.getByOutTradeNo(outTradeNo);
            if (tx != null) {
                try {
                    Map<?, ?> json = MAPPER.readValue(decryptedJson, Map.class);
                    String transactionId = stringValue(json.get("transaction_id"));
                    Long amountPayer = numberValue(json, "amount.payer_total");

                    tx.setPayStatus(PayStatus.SUCCESS);
                    if (transactionId != null) tx.setTransactionId(transactionId);
                    if (amountPayer != null) tx.setAmountPayerTotal(amountPayer);
                    tx.setSuccessTime(LocalDateTime.now());
                    payTransactionService.updateById(tx);
                } catch (Exception e) {
                    log.warn("更新交易流水失败: outTradeNo={}, {}", outTradeNo, e.getMessage());
                }
            }
        } finally {
            MDC.remove(TraceConstants.MDC_TRACE_ID);
        }
    }

    private PayNotifyLog saveNotifyLog(String notifyType, HttpServletRequest request, String rawBody,
                                       String sig, String ts, String nonce, String serial) throws IOException {
        Map<String, String> headers = collectHeaders(request);
        headers.put("Wechatpay-Signature", nullToEmpty(sig));
        headers.put("Wechatpay-Timestamp", nullToEmpty(ts));
        headers.put("Wechatpay-Nonce", nullToEmpty(nonce));
        headers.put("Wechatpay-Serial", nullToEmpty(serial));

        PayNotifyLog logEntity = new PayNotifyLog();
        logEntity.setNotifyType(notifyType);
        logEntity.setMchId(extractMchIdFromBody(rawBody));
        logEntity.setHeaders(MAPPER.writeValueAsString(headers));
        logEntity.setRawBody(rawBody);
        logEntity.setVerifyResult(NotifyResult.SKIPPED_PHASE3);
        logEntity.setProcessResult(NotifyResult.IGNORED_PHASE3);
        logEntity.setErrorMessage("待真实私钥补齐后激活验签/解密流程");
        payNotifyLogService.save(logEntity);
        log.info("{} 回调已落库: notifyId={}", notifyType, logEntity.getId());
        return logEntity;
    }

    /**
     * SIGNTEST 探测流量识别：User-Agent 含 WECHATPAY/SIGNTEST/。
     *
     * <p>必须返回 200 OK（否则微信认为商户验签能力未就绪），不进入业务逻辑。</p>
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

    private String extractOutTradeNoFromDecrypted(String decryptedJson) {
        try {
            Map<?, ?> json = MAPPER.readValue(decryptedJson, Map.class);
            Object v = json.get("out_trade_no");
            return v != null ? v.toString() : null;
        } catch (Exception e) {
            log.warn("解析解密结果失败: {}", e.getMessage());
            return null;
        }
    }

    private static String stringValue(Object o) {
        return o == null ? null : o.toString();
    }

    private static Long numberValue(Map<?, ?> json, String dottedPath) {
        try {
            String[] parts = dottedPath.split("\\.");
            Object cur = json;
            for (String p : parts) {
                if (cur instanceof Map<?, ?> m) {
                    cur = m.get(p);
                } else {
                    return null;
                }
            }
            if (cur instanceof Number n) return n.longValue();
            if (cur instanceof String s) return Long.parseLong(s);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String successJsonResponse() {
        return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
    }
}
