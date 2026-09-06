package com.xwms.integration.external.es.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * ES 同步异步线程池配置
 *
 * <p>ES 同步为最终一致性，失败不阻塞主业务。线程池使用 CallerRunsPolicy 做背压， 队列满时由调用线程直接执行，避免消息丢失。
 */
@Configuration
@EnableAsync
public class EsAsyncConfig implements AsyncConfigurer {

    @Bean(name = "esSyncExecutor")
    public Executor esSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("es-sync-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
