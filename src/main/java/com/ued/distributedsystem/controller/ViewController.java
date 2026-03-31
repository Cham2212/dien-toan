package com.ued.distributedsystem.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody; // Thêm import này

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List; // Thêm import này
import java.util.ArrayList; // Thêm import này

@Controller
public class ViewController {

    @Value("${server.id:Server-Default}")
    private String serverId;

    @Value("${peer.servers:}") // Lấy danh sách peers từ cấu hình
    private List<String> peerServers;

    @GetMapping("/client-booking")
    public String clientBooking(Model model) {
        String ipAddress = "Unknown";
        try {
            // Lấy IP nội bộ của máy đang chạy
            ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            ipAddress = "127.0.0.1";
        }

        model.addAttribute("serverId", serverId);
        model.addAttribute("serverIp", ipAddress);
        return "client-booking";
    }

    // Endpoint mới để trả về danh sách các Server (bao gồm chính nó)
    @GetMapping("/api/servers/list")
    @ResponseBody // Trả về JSON
    public List<String> getServerList() {
        String currentServerUrl = "http://localhost:8080"; // Mặc định local
        try {
            currentServerUrl = "http://" + InetAddress.getLocalHost().getHostAddress() + ":8080";
        } catch (UnknownHostException e) {
        }

        List<String> allServers = new ArrayList<>();
        allServers.add(currentServerUrl + " (Máy này)"); // Thêm máy hiện tại

        if (peerServers != null) {
            allServers.addAll(peerServers); // Thêm các máy peers
        }
        return allServers;
    }

    @GetMapping("/server-dashboard")
    public String serverDashboard(Model model) {
        model.addAttribute("serverId", serverId);
        return "server-dashboard";
    }
}