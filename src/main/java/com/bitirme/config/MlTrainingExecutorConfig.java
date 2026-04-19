package com.bitirme.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * ML eğitiminin HTTP thread'inden ayrılması için tek iş parçacıklı yürütücü (üst üste train isteği sıraya girer).
 */
@Configuration
public class MlTrainingExecutorConfig {

    @Bean(name = "mlTrainingExecutor")
    public Executor mlTrainingExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(1);
        ex.setMaxPoolSize(1);
        ex.setQueueCapacity(4);
        ex.setThreadNamePrefix("ml-train-");
        ex.initialize();
        return ex;
    }
}
