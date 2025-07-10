package com.concentrix.asset.dto.request;

import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.enums.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CreateTransferRequest {

    @NotNull(message = "To Warehouse ID type cannot be null")
    Integer toWarehouseId;

    @NotNull(message = "From Warehouse ID type cannot be null")
    Integer fromWarehouseId;

    String note;

    @Valid
            @NotEmpty(message = "Items cannot be empty")
    List<TransactionItem> items;


}
