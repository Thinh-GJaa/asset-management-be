package com.concentrix.asset.dto.request;

import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.enums.ModelSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CreateModelRequest {
    @NotBlank(message = "Model name is required")
    String modelName;

    @NotBlank(message = "Manufacturer is required")
    String manufacturer;

    @NotNull(message = "Device type is required")
    DeviceType type;

    @NotBlank(message = "Description is required")
    String description;
}