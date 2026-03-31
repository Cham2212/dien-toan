package com.ued.distributedsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadConfig {
    @Bean
    public ExecutorService executorService() {
        // Tạo pool 10 luồng để xử lý việc gửi tin nhắn đồng bộ song song
        return Executors.newFixedThreadPool(10);
    }
}