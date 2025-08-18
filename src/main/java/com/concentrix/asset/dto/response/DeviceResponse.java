package com.concentrix.asset.dto.response;

import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceResponse implements Serializable {

    Integer deviceId;
    String serialNumber;
    String deviceName;
    String hostName;
    String seatNumber;
    String poId;
    LocalDate purchaseDate;

    ModelResponse model;
    UserResponse user;
    FloorResponse floor;
    WarehouseResponse warehouse;
    SiteResponse site;
    DeviceStatus status;
    String description;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ModelResponse implements Serializable {
        Integer modelId;
        String modelName;
        DeviceType type; // Thêm trường type để trả về loại thiết bị
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserResponse implements Serializable {
        String eid;
        String fullName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FloorResponse implements Serializable {
        Integer floorId;
        String floorName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class WarehouseResponse implements Serializable {
        Integer warehouseId;
        String warehouseName;
    }

}
