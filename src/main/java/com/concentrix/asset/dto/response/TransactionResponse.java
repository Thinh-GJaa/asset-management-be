package com.concentrix.asset.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class TransactionResponse {
    Integer transactionId;
    String transactionType;
    String fromWarehouse;
    String toWarehouse;
    LocalDateTime createdAt;
    String createdBy;
    String note;
}