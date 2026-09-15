package com.weichat.finance.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * MDC traceId 过滤器（HTTP 请求入口）。
 *
 * <h3>职责</h3>
 * <ol>
 *   <li>从 HTTP header 读取 traceId（优先级：X-Trace-Id → Request-Id（微信回调） → 自动生成）</li>
 *   <li>写入 MDC（线程绑定）</li>
 *   <li>回传到响应头 {@code X-Trace-Id}，便于客户端/上游链路追踪</li>
 *   <li>请求结束时清理 MDC，避免内存泄漏（{@link OncePerRequestFilter} 保证只执行一次）</li>
 * </ol>
 *
 * <h3>traceId 来源优先级</h3>
 * <ol>
 *   <li><strong>HTTP 头 X-Trace-Id</strong>（通用）：业务调用方或网关传入，便于跨服务追踪</li>
 *   <li><strong>微信回调 Request-Id 头</strong>：微信侧的请求 ID，沿用便于跨系统排障</li>
 *   <li><strong>自动生成 UUID</strong>：兜底</li>
 * </ol>
 *
 * <h3>调用栈效果</h3>
 * <pre>
 *   2026-09-15 14:00:00.123 INFO  [http-nio-8080-exec-1] c.w.f.controller.JsapiController - [HTTP_a1b2c3d4] 收到下单请求
 *   2026-09-15 14:00:00.456 INFO  [http-nio-8080-exec-1] c.w.f.service.PayOrderService - [HTTP_a1b2c3d4] 落库成功
 *   2026-09-15 14:00:00.789 INFO  [http-nio-8080-exec-1] c.w.f.payment.MockJsapiService - [HTTP_a1b2c3d4] 返回 prepay_id
 *   ...同一请求内所有日志带相同 traceId
 * </pre>
 *
 * @author panhw
 * @since 2026-09-15
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MdcTraceIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MdcTraceIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 按优先级解析 traceId
            String traceId = resolveTraceId(request);

            // 2. 写入 MDC（线程绑定）
            MDC.put(TraceConstants.MDC_TRACE_ID, traceId);

            // 3. 回传响应头（便于客户端透传到下游）
            response.setHeader(TraceConstants.HEADER_TRACE_ID, traceId);

            log.debug("MDC traceId 已绑定: {}", traceId);
            filterChain.doFilter(request, response);
        } finally {
            // 4. 清理 MDC，避免线程复用时残留（Servlet 线程池会复用线程）
            MDC.remove(TraceConstants.MDC_TRACE_ID);
        }
    }

    /**
     * 解析 traceId（优先级：X-Trace-Id → Request-Id → 自动生成）。
     */
    private String resolveTraceId(HttpServletRequest request) {
        String traceId;

        // 1. 优先用业务侧传的 X-Trace-Id（覆盖所有场景）
        traceId = request.getHeader(TraceConstants.HEADER_TRACE_ID);
        if (isValidTraceId(traceId)) {
            return TraceConstants.PREFIX_HTTP + traceId;
        }

        // 2. 微信回调：用微信侧的 Request-Id
        traceId = request.getHeader(TraceConstants.WECHAT_HEADER_REQUEST_ID);
        if (isValidTraceId(traceId)) {
            return TraceConstants.PREFIX_WECHAT + traceId;
        }

        // 3. 兜底：自动生成（去掉横线，便于日志对齐）
        return TraceConstants.PREFIX_HTTP + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * traceId 合法性校验（非空 + 长度限制 + 字符集白名单）。
     *
     * <p>防止恶意 header 注入日志系统：限制长度、限制字符。</p>
     */
    private boolean isValidTraceId(String traceId) {
        if (traceId == null || traceId.isEmpty()) {
            return false;
        }
        // 限制长度 1-128
        if (traceId.length() > 128) {
            return false;
        }
        // 只允许字母、数字、横线、下划线、点
        for (int i = 0; i < traceId.length(); i++) {
            char c = traceId.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.')) {
                return false;
            }
        }
        return true;
    }
}
