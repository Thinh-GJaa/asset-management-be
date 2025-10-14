package com.concentrix.asset.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class DeviceChangeDetail implements Serializable {
    String fromDate;
    String toDate;
    Integer totalAdded;
    Integer totalRemoved;
    Integer netChange;
    List<DeviceInfo> addedDevices;
    List<DeviceInfo> removedDevices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE)
    public static class DeviceInfo implements Serializable {
        Integer deviceId;
        String serialNumber;
        String deviceName;
        String modelName;
        String type;
        String status;
        String siteName;
    }
}
