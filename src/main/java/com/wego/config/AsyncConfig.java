package com.wego.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async processing configuration.
 *
 * Provides a dedicated thread pool for long-running transport calculations
 * (Google Maps API calls with rate limiting).
 *
 * @contract
 *   - post: "transportExecutor" bean available for @Async methods
 *   - post: CallerRunsPolicy ensures tasks execute even when pool is full
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool for transport recalculation tasks.
     *
     * Small pool (2-4 threads) since these are I/O-bound API calls with rate limiting.
     * CallerRunsPolicy degrades to synchronous execution when pool is saturated,
     * ensuring no tasks are dropped.
     */
    @Bean(name = "transportExecutor")
    public Executor transportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("transport-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("Transport thread pool initialized: core=2, max=4, queue=10");
        return executor;
    }
}
