package com.concentrix.asset.enums;

public enum TransactionType {
    IMPORT("Nhập kho"),
    EXPORT("Xuất kho"),
    TRANSFER("Chuyển kho"),
    ADJUSTMENT("Điều chỉnh"),
    MAINTENANCE_EXPORT("Xuất bảo trì"),
    DISPOSAL_EXPORT("Xuất thanh lý"),
    RETURN("Trả về");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 