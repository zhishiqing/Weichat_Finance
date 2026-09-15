package com.weichat.finance.config;

import com.weichat.finance.trace.MdcTaskDecorator;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务线程池配置（含 MDC 传播）。
 *
 * <p>Spring 默认的 {@code SimpleAsyncTaskExecutor} 每次执行任务都创建新线程（性能差），
 * 且 <strong>不继承父线程 MDC</strong>，导致异步日志丢失 traceId。</p>
 *
 * <p>本配置提供一个线程池，并设置 {@link MdcTaskDecorator} 让异步线程继承 MDC。</p>
 *
 * @author panhw
 * @since 2026-09-15
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-");
        executor.setTaskDecorator(new MdcTaskDecorator());  // 关键：让异步线程继承 MDC
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects) -> {
            // 异步异常统一记录
            org.slf4j.LoggerFactory.getLogger(AsyncConfig.class)
                .error("异步任务执行失败: method={}, args={}", method.getName(), objects, throwable);
        };
    }
}
