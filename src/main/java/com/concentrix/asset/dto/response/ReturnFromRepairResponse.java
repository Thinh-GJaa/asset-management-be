package com.concentrix.asset.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReturnFromRepairResponse implements Serializable {
    Integer transactionId;
    WarehouseResponse toWarehouse;
    UserResponse createdBy;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String note;
    List<TransactionItemsResponse> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserResponse implements Serializable {
        String eid;
        String fullName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class WarehouseResponse implements Serializable {
        String warehouseId;
        String warehouseName;
    }
}