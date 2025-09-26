package com.concentrix.asset.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportDetailResponse implements Serializable {
    int total;
    List<SiteNode> sites;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SiteNode implements Serializable {
        Integer siteId;
        String siteName;
        int total;
        List<FloorNode> floors;
        List<WarehouseNode> warehouses;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FloorNode implements Serializable {
        Integer floorId;
        String floorName;
        int total;
        List<TypeNode> types;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class WarehouseNode implements Serializable {
        Integer warehouseId;
        String warehouseName;
        int total;
        List<TypeNode> types;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TypeNode implements Serializable {
        String type;
        int total;
        List<ModelNode> models;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ModelNode implements Serializable {
        Integer modelId;
        String modelName;
        boolean isSerial;
        int total;
        List<String> serials; // chỉ có nếu là serial
    }
}