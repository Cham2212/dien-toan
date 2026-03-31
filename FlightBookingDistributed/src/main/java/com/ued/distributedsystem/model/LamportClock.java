package com.ued.distributedsystem.model;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class LamportClock {
    private final AtomicInteger clock = new AtomicInteger(0);

    // Tăng đồng hồ cho một sự kiện nội bộ hoặc trước khi gửi tin nhắn
    public int tick() {
        return clock.incrementAndGet();
    }

    // Cập nhật đồng hồ khi nhận được tin nhắn từ server khác
    public synchronized void update(int receivedTime) {
        int currentTime = clock.get();
        int newTime = Math.max(currentTime, receivedTime) + 1;
        clock.set(newTime);
    }

    public int getTime() {
        return clock.get();
    }
}