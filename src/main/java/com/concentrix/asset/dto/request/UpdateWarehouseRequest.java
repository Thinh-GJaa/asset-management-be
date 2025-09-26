package com.concentrix.asset.dto.request;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateWarehouseRequest {

    @NotNull(message = "Warehouse ID cannot be null")
            @Min(1)
    Integer warehouseId;

    String warehouseName;

    @Min(1)
    Integer siteId;
}
