package com.concentrix.asset.dto.response;

import com.concentrix.asset.dto.request.POItem;
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
public class POResponse implements Serializable{

    String poId;
    VendorResponse vendor;
    WarehouseResponse warehouse;
    UserResponse createdBy;
    String note;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<POItemResponse> poItems;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class VendorResponse implements Serializable{
        Integer vendorId;
        String vendorName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class WarehouseResponse implements Serializable{
        Integer warehouseId;
        String warehouseName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserResponse implements Serializable{
        String eid;
        String fullName;
    }



}
