package com.archiveat.server.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 비동기 작업용 Thread Pool 설정
     * 
     * - Core Pool Size: 5 (기본적으로 유지되는 스레드 수)
     * - Max Pool Size: 10 (최대 생성 가능한 스레드 수)
     * - Queue Capacity: 25 (대기 큐에 들어갈 수 있는 작업 수)
     * 
     * LLM 요약 작업은 5-10초 소요되므로,
     * 동시에 많은 요청이 들어와도 순차적으로 처리됩니다.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("async-newsletter-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
