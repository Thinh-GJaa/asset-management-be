package com.concentrix.asset.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.cglib.core.Local;

import java.io.Serializable;
import java.time.LocalDate;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class POItem implements Serializable {

    @NotBlank(message = "Device name cannot be blank")
    String deviceName;

    @NotNull(message = "Model ID cannot be null")
    Integer modelId;

    String serialNumber;

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity;

    LocalDate startDate;

    LocalDate endDate;


}

