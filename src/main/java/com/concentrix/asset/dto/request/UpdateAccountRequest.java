package com.concentrix.asset.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UpdateAccountRequest {

    @NotNull(message = "Account ID is required")
    Integer accountId;

    String accountName;

    String accountCode;

    String ownerEid; // EID of the user who will be the owner

    String description;
}
