package com.concentrix.asset.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class AccountResponse {

    Integer accountId;
    String accountName;
    String accountCode;
    String description;
    UserResponse owner;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    UserResponse createdBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE)
    public static class UserResponse {
        String eid;
        String fullName;
    }
}