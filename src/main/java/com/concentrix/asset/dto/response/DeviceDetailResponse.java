package com.concentrix.asset.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeviceDetailResponse implements Serializable {
    Integer deviceId;
    String deviceName;
    String serialNumber;
    String modelName;
    String status;
    String warehouseName;
    String floorName;
    String assignedUserEid;
    String assignedUserName;
    String vendorName;
    String note;
    // Thêm các trường khác nếu cần
}