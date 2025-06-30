package com.concentrix.asset.enums;

public enum NotificationStatus {
    PENDING("Chờ xử lý"),
    PROCESSED("Đã xử lý"),
    CANCELLED("Đã hủy"),
    EXPIRED("Hết hạn");

    private final String description;

    NotificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 