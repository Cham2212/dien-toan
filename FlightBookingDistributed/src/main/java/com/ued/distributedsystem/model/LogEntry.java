package com.ued.distributedsystem.model;

public class LogEntry {
    private String type; // BOOKING, SYNC, ERROR, INFO
    private String message;
    private int lamportTime;
    private String timestamp;

    public LogEntry(String type, String message, int lamportTime, String timestamp) {
        this.type = type;
        this.message = message;
        this.lamportTime = lamportTime;
        this.timestamp = timestamp;
    }

    // Getters / Setters (Cần thiết để Spring trả về JSON)
    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public int getLamportTime() {
        return lamportTime;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
