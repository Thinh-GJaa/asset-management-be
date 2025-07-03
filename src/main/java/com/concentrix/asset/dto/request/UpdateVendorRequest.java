package com.concentrix.asset.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateVendorRequest implements Serializable {
    @NotNull(message = "Vendor ID cannot be null")
    @Min(1)
    private Integer vendorId;
    private String vendorName;
    private String address;
    private String phoneNumber;
    private String email;
}