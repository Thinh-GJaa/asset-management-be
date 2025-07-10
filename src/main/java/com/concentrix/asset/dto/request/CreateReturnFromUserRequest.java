package com.concentrix.asset.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CreateReturnFromUserRequest {
    @NotNull(message = "To Warehouse ID cannot be null")
    Integer toWarehouseId;

    @NotBlank(message = "User ID cannot be blank")
    String eid;

    String note;

    @Valid
    @NotEmpty(message = "Items cannot be empty")
    List<TransactionItem> items;
}