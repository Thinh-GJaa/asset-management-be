package com.concentrix.asset.enums;

public enum DisposalMethod {
    BAN("Bán"),
    VUT_BO("Vứt bỏ"),
    TAI_CHE("Tái chế"),
    CHUYEN_GIAO("Chuyển giao"),
    PHAN_PHAT("Phân phát"),
    THU_HOI("Thu hồi");

    private final String description;

    DisposalMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 