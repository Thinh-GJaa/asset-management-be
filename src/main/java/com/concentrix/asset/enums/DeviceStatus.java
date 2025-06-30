package com.concentrix.asset.enums;

public enum DeviceStatus {
    AVAILABLE("Có sẵn"),
    IN_USE("Đang sử dụng"),
    MAINTENANCE("Đang bảo trì"),
    DISPOSED("Đã thanh lý"),
    DAMAGED("Hỏng hóc"),
    RESERVED("Đã đặt trước"),
    TRANSFERRED("Đã chuyển đi");

    private final String description;

    DeviceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 