package com.ued.distributedsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ued.distributedsystem.model.LamportClock;
import com.ued.distributedsystem.model.Booking;
import com.ued.distributedsystem.service.LogService;
import com.ued.distributedsystem.repository.BookingRepository;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private LamportClock lamportClock;

    @Autowired
    private LogService logService;

    @Autowired
    private BookingRepository bookingRepository;

    @Value("#{'${peer.servers}'.split(',')}")
    private List<String> peerServers;

    @Value("${server.id:Cloud-Server-Duong}")
    private String serverId;

    // Giới hạn Thread Pool để tránh quá tải tài nguyên trên Render
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    
    // Cấu hình RestTemplate có Timeout để tránh treo Server khi Node bạn Offline
    private final RestTemplate restTemplate = createRestTemplate();

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); // 3 giây kết nối
        factory.setReadTimeout(3000);    // 3 giây chờ phản hồi
        return new RestTemplate(factory);
    }

    /**
     * GỬI LOG LÊN DASHBOARD REAL-TIME
     */
    private void sendToDashboard(String type, String message, int clock) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("type", type);
        logData.put("message", message);
        logData.put("lamportClock", clock);
        logData.put("serverId", serverId);
        messagingTemplate.convertAndSend("/topic/logs/" + serverId, logData);
    }

    // 1. XỬ LÝ ĐẶT VÉ TỪ CLIENT (Giao diện đặt vé)
    @PostMapping("/bookings")
    public ResponseEntity<Map<String, Object>> handleClientBooking(@RequestBody Map<String, String> payload) {
        String flightId = payload.get("flightId");
        String userId = payload.get("userId");

        // Tăng clock nội bộ khi có sự kiện mới
        int currentTime = lamportClock.tick();

        sendToDashboard("CLIENT", "Nhận lệnh ĐẶT VÉ [" + flightId + "] từ: " + userId, currentTime);
        logService.addLog("CLIENT", "Request từ " + userId, currentTime);

        try {
            // Lưu vào Database nội bộ (MongoDB/MySQL)
            String dbName = "DB_" + serverId.replace("Cloud-Server-", "");
            
            Booking newBooking = new Booking();
            newBooking.setPassengerName(userId);
            newBooking.setFlightId(flightId);
            newBooking.setLamportTimestamp(currentTime);
            newBooking.setServerId(serverId);

            bookingRepository.save(newBooking);
            sendToDashboard("DATABASE", "Ghi thành công vào " + dbName, currentTime);
        } catch (Exception e) {
            sendToDashboard("ERROR", "Lỗi ghi DB: " + e.getMessage(), currentTime);
        }

        // Bắt đầu đồng bộ sang các Server khác (Async)
        sendToDashboard("TRANSACTION", "BẮT ĐẦU quy trình đồng bộ liên Server...", currentTime);
        broadcastSyncMessage(flightId, userId, currentTime);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Thành công");
        response.put("lamportClock", currentTime);
        response.put("serverId", serverId);

        return ResponseEntity.ok(response);
    }

    // 2. XỬ LÝ NHẬN ĐỒNG BỘ TỪ SERVER BẠN
    @PostMapping("/sync")
    public void handleSyncMessage(@RequestParam String serverOrigin,
                                @RequestParam String flightId,
                                @RequestParam String userId,
                                @RequestParam int senderTime) {

        // Cập nhật clock theo Lamport: max(local, sender) + 1
        lamportClock.update(senderTime);
        int newTime = lamportClock.getTime();

        sendToDashboard("LAMPORT", "Nhận SYNC từ " + serverOrigin + " (Clock gửi: " + senderTime + ")", newTime);
        
        try {
            Booking syncBooking = new Booking();
            syncBooking.setPassengerName(userId);
            syncBooking.setFlightId(flightId);
            syncBooking.setLamportTimestamp(senderTime); // Giữ timestamp gốc của người gửi
            syncBooking.setServerId(serverOrigin);
            bookingRepository.save(syncBooking);
            
            sendToDashboard("DATABASE", "Đã đồng bộ vé [" + flightId + "] từ " + serverOrigin, newTime);
        } catch (Exception e) {
            sendToDashboard("ERROR", "Lỗi đồng bộ DB: " + e.getMessage(), newTime);
        }
    }

    @GetMapping("/logs/private-view")
    public List<String> getLogs() {
        return logService.getAllLogs();
    }

    // 3. PHÁT TIN ĐỒNG BỘ (BROADCAST)
    private void broadcastSyncMessage(String flightId, String userId, int currentTime) {
        int nodeIndex = 1;
        for (String peerUrl : peerServers) {
            if (peerUrl == null || peerUrl.trim().isEmpty()) continue;

            final int index = nodeIndex++;
            executor.submit(() -> {
                try {
                    sendToDashboard("NETWORK", "Đang truyền tin tới Node " + index + " (" + peerUrl + ")", currentTime);
                    
                    // Sử dụng UriComponentsBuilder để tránh lỗi ký tự đặc biệt trong URL
                    String url = UriComponentsBuilder.fromHttpUrl(peerUrl + "/api/sync")
                            .queryParam("serverOrigin", serverId)
                            .queryParam("flightId", flightId)
                            .queryParam("userId", userId)
                            .queryParam("senderTime", currentTime)
                            .toUriString();

                    restTemplate.postForObject(url, null, String.class);
                    
                    sendToDashboard("SYNC", "Node " + index + " xác nhận: ĐÃ NHẬN", currentTime);
                } catch (Exception e) {
                    sendToDashboard("ERROR", "Node " + index + " thất bại (Offline hoặc Timeout)", currentTime);
                }
            });
        }
    }
}