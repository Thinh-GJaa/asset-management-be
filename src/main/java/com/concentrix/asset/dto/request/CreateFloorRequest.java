package com.concentrix.asset.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CreateFloorRequest {

    @NotBlank(message = "Floor name cannot be blank")
    String floorName;

    @NotNull(message = "Site Id cannot be null")
    Integer siteId;

    @NotNull(message = "Account Id cannot be null")
    Integer accountId;

    String description;
}