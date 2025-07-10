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
public class ReportSummaryResponse implements Serializable {
    String siteName;
    Integer siteId;
    List<WarehouseSummary> warehouses;
    List<FloorSummary> floors;
    List<TypeSummary> types;
    List<ModelSummary> models;
    DeviceStatusCount statusCount;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class WarehouseSummary implements Serializable {
        Integer warehouseId;
        String warehouseName;
        DeviceStatusCount statusCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FloorSummary implements Serializable {
        Integer floorId;
        String floorName;
        DeviceStatusCount statusCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TypeSummary implements Serializable {
        String type;
        DeviceStatusCount statusCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ModelSummary implements Serializable {
        Integer modelId;
        String modelName;
        DeviceStatusCount statusCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DeviceStatusCount implements Serializable {
        Integer inUse;
        Integer inStock;
        Integer assigned;
        Integer disposed;
        Integer ewaste;
    }
}
