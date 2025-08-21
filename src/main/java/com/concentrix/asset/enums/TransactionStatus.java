package com.concentrix.asset.enums;

public enum TransactionStatus {
    PENDING, // Đang vận chuyển, chờ xác nhận
    APPROVED, // Đã duyệt đơn hàng
    CONFIRMED, // Đã xác nhận nhận hàng
    CANCELED, // Đã hủy đơn hàng
}
