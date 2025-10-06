package com.concentrix.asset.dto.request;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;


@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LaptopBadgeRequest {

    @NotNull
    Integer transactionId;

    @NotNull
    @NotEmpty(message = "List serial is required")
    List<String> listSerial;
}
