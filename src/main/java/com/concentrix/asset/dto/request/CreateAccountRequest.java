package com.concentrix.asset.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CreateAccountRequest {

    @NotBlank(message = "Account name is required")
    String accountName;

    @NotBlank(message = "Account code is required")
    String accountCode;

    @NotBlank(message = "Owner EID is required")
    String ownerEid; // EID of the user who will be the owner

    String description;
}
