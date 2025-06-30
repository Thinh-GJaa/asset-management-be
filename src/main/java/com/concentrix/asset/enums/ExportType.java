package com.concentrix.asset.enums;

public enum ExportType {
    CAP_PHAT("Cấp phát"),
    BAO_TRI("Bảo trì"),
    THANH_LY("Thanh lý"),
    CHUYEN_SITE("Chuyển site"),
    DIEU_CHUYEN("Điều chuyển");

    private final String description;

    ExportType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 