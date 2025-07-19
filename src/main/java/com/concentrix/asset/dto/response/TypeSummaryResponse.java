package com.concentrix.asset.dto.response;

import com.concentrix.asset.enums.DeviceType;
import lombok.Data;
import java.util.List;

@Data
public class TypeSummaryResponse {
    private DeviceType type;
    private int total;
    private List<ModelSummaryResponse> models;
}