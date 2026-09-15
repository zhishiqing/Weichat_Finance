package com.weichat.finance.trace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * 调度任务专用工具方法。
 *
 * <p>Spring 的 {@code @Scheduled} 任务默认运行在单线程 {@code scheduling-1}，
 * 所有任务共用同一个 traceId 反而会串扰。需要为每次任务执行生成独立 traceId，
 * 然后在任务结束清理 MDC。</p>
 *
 * <h3>用法</h3>
 * <pre>
 * &#64;Scheduled(cron = "0 0 3 * * ?")
 * public void dailyReconciliation() {
 *     String traceId = ScheduledTaskMdcHelper.startScheduledTask("dailyReconciliation");
 *     try {
 *         // 业务逻辑
 *     } finally {
 *         ScheduledTaskMdcHelper.endScheduledTask();
 *     }
 * }
 * </pre>
 *
 * @author panhw
 * @since 2026-09-15
 */
public final class ScheduledTaskMdcHelper {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskMdcHelper.class);

    private ScheduledTaskMdcHelper() {
        throw new AssertionError("工具类禁止实例化");
    }

    /**
     * 开始一个调度任务：生成 traceId 并写入 MDC。
     *
     * @param taskName 任务名（用于 traceId，便于区分任务类型）
     * @return 生成的 traceId，便于日志传递
     */
    public static String startScheduledTask(String taskName) {
        String traceId = TraceConstants.PREFIX_SCHED + taskName + "_"
            + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(TraceConstants.MDC_TRACE_ID, traceId);
        log.info("[调度任务开始] taskName={}, traceId={}", taskName, traceId);
        return traceId;
    }

    /**
     * 结束调度任务：清理 MDC。
     */
    public static void endScheduledTask() {
        MDC.remove(TraceConstants.MDC_TRACE_ID);
    }
}
