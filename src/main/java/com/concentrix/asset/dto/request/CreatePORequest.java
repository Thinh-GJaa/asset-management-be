package com.concentrix.asset.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CreatePORequest {

    @NotNull(message = "Vendor Id is required")
    Integer vendorId;

    @NotNull(message = "Warehouse Id is required")
    Integer warehouseId;

    @NotBlank(message = "Purchase Order Id is required")
    String poId;

    @NotEmpty(message = "Purchase Order items cannot be empty")
    @Valid
    List<POItem> items;

    @NotNull(message = "Purchase Order date cannot be null")
    LocalDate poDate;

    String note;

}