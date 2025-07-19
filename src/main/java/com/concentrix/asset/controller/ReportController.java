package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.response.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.service.DeviceService;
import com.concentrix.asset.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ReportController {

    ReportService reportService;
    DeviceService deviceService;

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

    @GetMapping("/status-summary")
    public ResponseEntity<Map<String, Map<String, Integer>>> getStatusSummaryAllSite() {
        return ResponseEntity.ok(reportService.getStatusSummaryAllSite());
    }

    @GetMapping("/without-serial")
    public ResponseEntity<ApiResponse<List<SiteDeviceWithoutSerialSummaryResponse>>> getWithoutSerialSummary(
            @RequestParam(value = "status", required = false) DeviceStatus status,
            @RequestParam(value = "type", required = false) DeviceType type,
            @RequestParam(value = "modelId", required = false) Integer modelId) {
        List<SiteDeviceWithoutSerialSummaryResponse> result = reportService.getWithoutSerialSummary(status, type,
                modelId);
        ApiResponse<List<SiteDeviceWithoutSerialSummaryResponse>> response = ApiResponse
                .<List<SiteDeviceWithoutSerialSummaryResponse>>builder()
                .message("Get without serial summary successfully")
                .data(result)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/with-serial")
    public ResponseEntity<ApiResponse<List<TypeSummaryResponse>>> getWithSerialSummary(
            @RequestParam(value = "siteId", required = false) Integer siteId,
            @RequestParam(value = "floorId", required = false) Integer floorId,
            @RequestParam(value = "status", required = false) DeviceStatus status,
            @RequestParam(value = "type", required = false) DeviceType type,
            @RequestParam(value = "modelId", required = false) Integer modelId) {
        List<TypeSummaryResponse> result = reportService.getWithSerialSummary(siteId, status, floorId, type, modelId);
        ApiResponse<List<TypeSummaryResponse>> response = ApiResponse
                .<List<TypeSummaryResponse>>builder()
                .message("Get with serial summary successfully")
                .data(result)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/device-list")
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> getDeviceListForReport(
            @RequestParam(value = "siteId", required = false) Integer siteId,
            @RequestParam(value = "status", required = false) DeviceStatus status,
            @RequestParam(value = "floorId", required = false) Integer floorId,
            @RequestParam(value = "type", required = false) DeviceType type,
            @RequestParam(value = "modelId", required = false) Integer modelId) {
        List<DeviceResponse> result = reportService.getDeviceListForReport(siteId, status, floorId, type, modelId);
        ApiResponse<List<DeviceResponse>> response = ApiResponse.<List<DeviceResponse>>builder()
                .message("Get device list for report successfully")
                .data(result)
                .build();
        return ResponseEntity.ok(response);
    }
}
