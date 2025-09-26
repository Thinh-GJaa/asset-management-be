package com.concentrix.asset.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UpdateDeviceRequest {

    @NotNull(message = "Device ID cannot be null")
    Integer deviceId;

    String deviceName;

    String serialNumber;

    Integer modelId;

}