package com.concentrix.asset.dto.request;

import com.concentrix.asset.enums.DeviceType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UpdateModelRequest implements Serializable {

    @NotNull(message = "Model ID cannot be null")
    Integer modelId;

    String modelName;
    String manufacturer;
    DeviceType type;
    String description;
}