package com.weichat.finance.trace;

/**
 * MDC 日志追踪相关常量。
 *
 * <p>统一管理 MDC key 名 / HTTP header 名，避免硬编码字符串散落各文件。</p>
 *
 * @author panhw
 * @since 2026-09-15
 */
public final class TraceConstants {

    /**
     * MDC 中的 traceId key（对应 logback pattern 的 {@code %X{traceId}}）。
     */
    public static final String MDC_TRACE_ID = "traceId";

    /**
     * HTTP header 名：
     * <ul>
     *   <li>对外 API：客户端传 {@code X-Trace-Id}（可透传前端 trace）</li>
     *   <li>微信回调：优先读 {@code Request-Id}（微信侧的请求 ID，便于跨系统追踪）</li>
     * </ul>
     */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /**
     * 微信回调的请求 ID header（来自微信支付 V3 回调）。
     */
    public static final String WECHAT_HEADER_REQUEST_ID = "Request-Id";

    /**
     * traceId 前缀（便于在日志中快速识别）：
     * <ul>
     *   <li>{@code HTTP_}：来自 HTTP 请求的 traceId</li>
     *   <li>{@code SCHED_}：来自调度任务的 traceId</li>
     *   <li>{@code WECH_}：来自微信回调的 traceId（沿用微信的 Request-Id）</li>
     * </ul>
     */
    public static final String PREFIX_HTTP = "HTTP_";
    public static final String PREFIX_SCHED = "SCHED_";
    public static final String PREFIX_WECHAT = "WECH_";

    private TraceConstants() {
        throw new AssertionError("常量类禁止实例化");
    }
}
