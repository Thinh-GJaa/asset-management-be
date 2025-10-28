package com.concentrix.asset.dto.response;

import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ChangeStatusResponse {
    Integer transactionId;
    TransactionType transactionType;
    DeviceStatus newStatus;
    String note;
    LocalDateTime createdAt;
    UserResponse createdBy;
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

}

