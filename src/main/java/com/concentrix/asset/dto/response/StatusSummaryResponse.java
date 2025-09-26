package com.concentrix.asset.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StatusSummaryResponse {
    private List<TypeSummaryResponse> type;
    private List<ModelSummaryResponse> model;
    private List<SiteSummaryResponse> site;
} 