package com.concentrix.asset.dto.request;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateWarehouseRequest {

    @NotBlank(message = "Site name is required")
    String warehouseName;

    @NotNull(message = "Site ID cannot be null")
    @Min(1)
    Integer siteId;

    String description;
}
