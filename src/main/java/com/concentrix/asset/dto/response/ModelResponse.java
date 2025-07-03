package com.concentrix.asset.dto.response;

import com.concentrix.asset.enums.DeviceType;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.concentrix.asset.enums.ModelSize;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ModelResponse implements Serializable {
    Integer modelId;
    String modelName;
    String manufacturer;
    String description;
    DeviceType type;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}