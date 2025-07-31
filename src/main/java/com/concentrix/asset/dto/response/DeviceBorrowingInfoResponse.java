package com.concentrix.asset.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceBorrowingInfoResponse implements Serializable {

    String eid;
    String fullName;
    List<DeviceInfo> devices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeviceInfo implements Serializable {
        String serialNumber;
        String deviceName;
        Integer quantity; // null hoặc 1 với serial, >1 với không serial
        Integer modelId; // Thêm trường này
        String modelName; // Thêm trường này
    }
}