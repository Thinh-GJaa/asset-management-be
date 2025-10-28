package com.concentrix.asset.dto.request;

import com.concentrix.asset.enums.DeviceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CreateChangeStatusRequest {

    Integer warehouseId;

    @NotNull(message = "New status cannot be null")
    DeviceStatus newStatus;

    String note;

    @Valid
    @NotEmpty(message = "Items cannot be empty")
    List<String> items; // List Serial number

}

