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
public class CreateAssignmentRequest {

    @NotNull(message = "From Warehouse ID type cannot be null")
    Integer fromWarehouseId;

    @NotBlank(message = "Employee ID cannot be blank")
    String eid;

    String note;

    @Valid
    @NotEmpty(message = "Items cannot be empty")
    List<TransactionItem> items;


}
