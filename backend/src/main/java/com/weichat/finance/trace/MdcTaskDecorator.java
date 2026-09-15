package com.weichat.finance.trace;

import org.slf4j.MDC;

import java.util.Map;

/**
 * MDC 任务装饰器：将当前线程的 MDC 上下文传递到异步线程。
 *
 * <h3>解决的问题</h3>
 * Spring 的 {@code @Async} 注解或自定义线程池在新线程中执行任务时，
 * <strong>不会自动继承父线程的 MDC</strong>，导致异步日志丢失 traceId。
 *
 * <h3>用法</h3>
 * 注册到自定义线程池：
 * <pre>
 * ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 * executor.setTaskDecorator(new MdcTaskDecorator());
 * executor.initialize();
 * </pre>
 *
 * <h3>原理</h3>
 * Spring 的 {@code TaskDecorator} 在任务提交时被调用，可以包装原始 {@code Runnable}，
 * 在包装内：先保存新线程的 MDC（通常为空）→ 复制父线程 MDC → 执行原任务 → 恢复/清理。
 *
 * @author panhw
 * @since 2026-09-15
 */
public class MdcTaskDecorator implements org.springframework.core.task.TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 捕获父线程 MDC 快照
        Map<String, String> contextSnapshot = MDC.getCopyOfContextMap();
        return () -> {
            try {
                // 写入新线程 MDC
                if (contextSnapshot != null) {
                    MDC.setContextMap(contextSnapshot);
                } else {
                    MDC.clear();
                }
                runnable.run();
            } finally {
                // 清理，避免线程复用时残留
                MDC.clear();
            }
        };
    }
}
