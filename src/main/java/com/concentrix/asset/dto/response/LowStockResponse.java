package com.concentrix.asset.dto.response;

import com.concentrix.asset.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class LowStockResponse {

    Integer siteId;
    String siteName;
    List<LowStockType> lowStockTypes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE)
    public static class LowStockType {
        DeviceType type;
        Integer total;
        Integer available;
    }
}