package com.ued.distributedsystem.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "bookings")
public class Booking {
    @Id
    private String id;
    private String passengerName;
    private String flightId;
    private int lamportTimestamp; // Đổi sang int cho khớp với LamportClock của bạn
    private String serverId;

    // --- GETTER & SETTER (Phần thiếu dẫn đến lỗi của bạn) ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getFlightId() {
        return flightId;
    }

    public void setFlightId(String flightId) {
        this.flightId = flightId;
    }

    public int getLamportTimestamp() {
        return lamportTimestamp;
    }

    public void setLamportTimestamp(int lamportTimestamp) {
        this.lamportTimestamp = lamportTimestamp;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }
}