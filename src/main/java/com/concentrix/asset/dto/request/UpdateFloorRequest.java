package com.concentrix.asset.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UpdateFloorRequest implements Serializable {

    @NotNull(message = "Floor ID cannot be null")
    Integer floorId;

    String floorName;

    Integer siteId;

    Integer accountId;

    String description;

}