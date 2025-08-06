package com.concentrix.asset.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssetHandoverResponse {
    Integer transactionId;
    String itPerson;
    String location;
    String endUser;
    String msa;
    String employeeId;
    String ssoEmail;
    String assetType;
    String issueDate;
    String role;
    LocalDateTime createdAt;
    LocalDate returnDate;
    List<TransactionItemsResponse> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssetHandoverDetailResponse {
        Integer deviceId;
        String deviceName;
        String serialNumber;
        Integer quantity;

    }
}