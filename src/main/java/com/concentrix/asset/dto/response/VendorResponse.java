package com.concentrix.asset.dto.response;

import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class VendorResponse implements Serializable {
    Integer vendorId;
    String vendorName;
    String address;
    String phoneNumber;
    String email;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}