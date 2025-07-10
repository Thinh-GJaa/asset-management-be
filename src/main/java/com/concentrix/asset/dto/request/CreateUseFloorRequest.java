package com.concentrix.asset.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CreateUseFloorRequest {
    @NotNull(message = "From Warehouse ID cannot be null")
    Integer fromWarehouseId;

    @NotNull(message = "To Floor ID cannot be null")
    Integer toFloorId;

    String note;

    @Valid
    @NotEmpty(message = "Items cannot be empty")
    List<TransactionItem> items;
}