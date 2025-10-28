package com.concentrix.asset.enums;

public enum TransactionType {
    TRANSFER_SITE,            // chuyển tài sản
    DISPOSAL,            // thanh lý
    ASSIGNMENT,          // cấp phát
    REPAIR,              // bảo hành
    USE_FLOOR,           // sử dụng tại sàn làm việc
    TRANSFER_FLOOR,       //Chuyển giữa các
    E_WASTE,             // rác điện tử

    RETURN_FROM_USER,         // trả về sau khi sử dụng
    RETURN_FROM_REPAIR,      // trả về sau bảo hành/sửa chữa
    RETURN_FROM_FLOOR,        // trả về từ sàn làm việc
    
    CHANGE_STATUS            // thay đổi trạng thái thiết bị
}
