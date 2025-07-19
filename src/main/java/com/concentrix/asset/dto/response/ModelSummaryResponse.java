package com.concentrix.asset.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class ModelSummaryResponse {
    private Integer modelId;
    private String modelName;
    private int total;
    private List<SiteSummaryResponse> sites;
}