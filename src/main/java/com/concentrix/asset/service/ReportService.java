package com.concentrix.asset.service;

import com.concentrix.asset.dto.response.ReportSummaryResponse;
import com.concentrix.asset.dto.response.ReportDetailResponse;
import java.util.List;

public interface ReportService {
    List<ReportSummaryResponse> getReportSummary(Integer siteId);

    ReportDetailResponse getReportDetail(Integer siteId, Integer floorId, Integer warehouseId, String type,
            Integer modelId);
}