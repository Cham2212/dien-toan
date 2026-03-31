package com.ued.distributedsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

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
@CrossOrigin(origins = "*") // Fix lỗi "Node ngoại tuyến" do chặn CORS
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

    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * GỬI LOG LÊN DASHBOARD NEON
     */
    private void sendToDashboard(String type, String message, int clock) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("type", type);
        logData.put("message", message);
        logData.put("lamportClock", clock);
        messagingTemplate.convertAndSend("/topic/logs/" + serverId, logData);
    }

    // 1. XỬ LÝ ĐẶT VÉ TỪ CLIENT (Dùng @RequestBody để nhận JSON, fix lỗi undefined)
    @PostMapping("/bookings")
    public ResponseEntity<Map<String, Object>> handleClientBooking(@RequestBody Map<String, String> payload) {

        // Bóc dữ liệu từ JSON Body
        String flightId = payload.get("flightId");
        String userId = payload.get("userId");

        // Tăng đồng hồ Lamport
        int currentTime = lamportClock.tick();

        // Gửi Log - Hết lỗi undefined vì đã có dữ liệu từ payload
        sendToDashboard("BOOKING", "XÁC NHẬN: Khách [" + userId + "] đặt vé " + flightId, currentTime);
        logService.addLog("BOOKING", "Nhận yêu cầu từ " + userId + ". Flight: " + flightId, currentTime);

        try {
            // Lưu vào MongoDB Atlas
            Booking newBooking = new Booking();
            newBooking.setPassengerName(userId);
            newBooking.setFlightId(flightId);
            newBooking.setLamportTimestamp(currentTime);
            newBooking.setServerId(serverId);

            bookingRepository.save(newBooking);
            logService.addLog("INFO", "Ghi transaction vào MongoDB Cluster0", currentTime);
        } catch (Exception e) {
            sendToDashboard("ERROR", "Lỗi DB: " + e.getMessage(), currentTime);
        }

        // Phát tín hiệu đồng bộ cho các server khác (Node Trâm, Node Chung...)
        broadcastSyncMessage(flightId, userId, currentTime);

        // Trả về kết quả cho Frontend
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Thành công");
        response.put("lamportClock", currentTime);

        return ResponseEntity.ok(response);
    }

    // 2. XỬ LÝ ĐỒNG BỘ TỪ SERVER BẠN
    @PostMapping("/sync")
    public void handleSyncMessage(@RequestParam String serverOrigin,
            @RequestParam String flightId,
            @RequestParam String userId,
            @RequestParam int senderTime) {

        lamportClock.update(senderTime);
        int newTime = lamportClock.getTime();

        sendToDashboard("SYNC", "Nhận đồng bộ từ " + serverOrigin, newTime);
        logService.addLog("SYNC", "Đồng bộ Clock (L_max + 1) từ " + serverOrigin, newTime);
    }

    @GetMapping("/logs/private-view")
    public List<String> getLogs() {
        return logService.getAllLogs();
    }

    // 3. BROADCAST
    private void broadcastSyncMessage(String flightId, String userId, int currentTime) {
        for (String peerUrl : peerServers) {
            if (peerUrl == null || peerUrl.trim().isEmpty())
                continue;

            executor.submit(() -> {
                try {
                    String url = peerUrl + "/api/sync?serverOrigin=" + serverId
                            + "&flightId=" + flightId
                            + "&userId=" + userId
                            + "&senderTime=" + currentTime;

                    restTemplate.postForObject(url, null, String.class);
                } catch (Exception e) {
                    sendToDashboard("ERROR", "Node bạn offline: " + peerUrl, currentTime);
                }
            });
        }
    }
}