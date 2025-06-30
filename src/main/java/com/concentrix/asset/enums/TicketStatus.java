package com.concentrix.asset.enums;

public enum TicketStatus {
    PENDING("Chờ xử lý"),
    APPROVED("Đã duyệt"),
    REJECTED("Từ chối"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy"),
    IN_PROGRESS("Đang xử lý");

    private final String description;

    TicketStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 