package com.concentrix.asset.enums;

public enum DisposalReason {
    HONG_HONG("Hỏng hóc không sửa được"),
    CU_KY("Cũ kỹ, lạc hậu"),
    KHONG_SU_DUNG("Không còn sử dụng"),
    NANG_CAP("Thay thế bằng thiết bị mới"),
    HET_HAN("Hết hạn sử dụng"),
    AN_TOAN("Không đảm bảo an toàn");

    private final String description;

    DisposalReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 