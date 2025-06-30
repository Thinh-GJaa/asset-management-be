package com.concentrix.asset.enums;

public enum MaintenanceType {
    PREVENTIVE("Bảo trì phòng ngừa"),
    CORRECTIVE("Bảo trì sửa chữa"),
    EMERGENCY("Bảo trì khẩn cấp"),
    PERIODIC("Bảo trì định kỳ");

    private final String description;

    MaintenanceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 