package com.concentrix.asset.dto.response;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WarehouseResponse implements Serializable {

    Integer warehouseId;
    String warehouseName;
    SiteResponse site;
    String description;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SiteResponse implements Serializable {
        Integer siteId;
        String siteName;
    }

}
