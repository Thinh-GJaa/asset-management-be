package com.concentrix.asset.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionItem implements Serializable {

    @NotNull(message = "Serial number cannot be null")
    String serialNumber;

    @NotNull(message = "Model ID cannot be null")
    Integer modelId;

    @NotNull(message = "Quantity cannot be null")
    Integer quantity;

}

