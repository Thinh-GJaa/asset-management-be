package com.concentrix.asset.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CreateVendorRequest {
    @NotBlank(message = "Vendor name is required")
    String vendorName;

    @NotBlank(message = "Address is required")
    String address;

    @NotBlank(message = "Phone number is required")
    String phoneNumber;

    @NotBlank(message = "Email is required")
            @Email
    String email;
}