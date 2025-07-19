package com.concentrix.asset.dto.response;

import lombok.Data;
import java.util.List;

import com.concentrix.asset.enums.DeviceType;

@Data
public class DeviceWithoutSerialSummaryResponse {
    private DeviceType type;
    private Integer total;
    private List<ModelQuantity> models;

    @Data
    public static class ModelQuantity {
        private Integer modelId;
        private String modelName;
        private Integer quantity;
    }
}