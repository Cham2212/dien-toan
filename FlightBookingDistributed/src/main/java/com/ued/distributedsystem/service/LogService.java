package com.ued.distributedsystem.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class LogService {
    // CopyOnWriteArrayList an toàn cho multi-thread khi đọc/ghi liên tục
    private final List<String> logs = new CopyOnWriteArrayList<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public void addLog(String type, String message, int logicalTime) {
        String timestamp = LocalDateTime.now().format(formatter);
        // Định dạng log: [Thời gian] [Loại Log] [Lamport: T] Nội dung
        String formattedLog = String.format("[%s] [%s] [Lamport: %d] %s", timestamp, type, logicalTime, message);
        logs.add(formattedLog);

        // In ra console để dễ debug trên Render
        System.out.println(formattedLog);
    }

    public List<String> getAllLogs() {
        return logs;
    }
}