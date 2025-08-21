package com.concentrix.asset.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UpdateSeatNumberRequest {

    @NotBlank(message = "Serial number is required")
    String serialNumber;

    @NotBlank(message = "Seat number is required")
    String seatNumber;


}
