package com.concentrix.asset.controller;

import com.concentrix.asset.dto.response.ReportSummaryResponse;
import com.concentrix.asset.dto.response.ReportDetailResponse;
import com.concentrix.asset.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/summary")
    public List<ReportSummaryResponse> getReportSummary(
            @RequestParam(value = "siteId", required = false) Integer siteId) {
        return reportService.getReportSummary(siteId);
    }

    @GetMapping("/detail")
    public ReportDetailResponse getReportDetail(
            @RequestParam(value = "siteId", required = false) Integer siteId,
            @RequestParam(value = "floorId", required = false) Integer floorId,
            @RequestParam(value = "warehouseId", required = false) Integer warehouseId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "modelId", required = false) Integer modelId) {
        return reportService.getReportDetail(siteId, floorId, warehouseId, type, modelId);
    }
}
