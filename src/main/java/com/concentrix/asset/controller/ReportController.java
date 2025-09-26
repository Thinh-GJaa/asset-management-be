package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.response.DeviceResponse;
import com.concentrix.asset.dto.response.SiteDeviceWithoutSerialSummaryResponse;
import com.concentrix.asset.dto.response.SiteTypeChartResponse;
import com.concentrix.asset.dto.response.TypeSummaryResponse;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ReportController {

    ReportService reportService;

    @GetMapping("/status-summary")
    public ResponseEntity<Map<String, Map<String, Integer>>> getStatusSummaryAllSite() {
        return ResponseEntity.ok(reportService.getStatusSummaryAllSite());
    }

    @GetMapping("/without-serial")
    public ResponseEntity<ApiResponse<List<SiteDeviceWithoutSerialSummaryResponse>>> getWithoutSerialSummary(
            @RequestParam(value = "status", required = false) DeviceStatus status,
            @RequestParam(value = "type", required = false) DeviceType type,
            @RequestParam(value = "modelId", required = false) Integer modelId) {
        List<SiteDeviceWithoutSerialSummaryResponse> result = reportService.getWithoutSerialSummary(status,
                type,
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
            @RequestParam(value = "ownerId", required = false) Integer ownerId,
            @RequestParam(value = "accountId", required = false) Integer accountId,
            @RequestParam(value = "type", required = false) DeviceType type,
            @RequestParam(value = "modelId", required = false) Integer modelId,
            @RequestParam(value = "isOutOfWarranty", required = false) Boolean isOutOfWarranty,
            @RequestParam(value = "ageRange", required = false) String ageRange) {
        List<TypeSummaryResponse> result = reportService.getWithSerialSummary(
                siteId, status, floorId, ownerId, accountId, type, modelId, isOutOfWarranty, ageRange);
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
            @RequestParam(value = "ownerId", required = false) Integer ownerId,
            @RequestParam(value = "floorId", required = false) Integer floorId,
            @RequestParam(value = "accountId", required = false) Integer accountId,
            @RequestParam(value = "type", required = false) DeviceType type,
            @RequestParam(value = "modelId", required = false) Integer modelId,
            @RequestParam(value = "isOutOfWarranty", required = false) Boolean isOutOfWarranty,
            @RequestParam(value = "ageRange", required = false) String ageRange) {
        List<DeviceResponse> result = reportService.getDeviceListForReport(
                siteId, status, floorId, ownerId, accountId, type, modelId, isOutOfWarranty, ageRange);
        ApiResponse<List<DeviceResponse>> response = ApiResponse.<List<DeviceResponse>>builder()
                .message("Get device list for report successfully")
                .data(result)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/with-serial/chart/{status}")
    public ResponseEntity<ApiResponse<List<SiteTypeChartResponse>>> getSiteTypeChartWithSerial(
            @PathVariable DeviceStatus status) {
        List<SiteTypeChartResponse> result = reportService.getSiteTypeChartWithSerial(status);
        ApiResponse<List<SiteTypeChartResponse>> response = ApiResponse
                .<List<SiteTypeChartResponse>>builder()
                .message("Get site type chart successfully")
                .data(result)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/without-serial/chart/{status}")
    public ResponseEntity<ApiResponse<List<SiteTypeChartResponse>>> getSiteTypeChartWithoutSerial(
            @PathVariable DeviceStatus status) {
        List<SiteTypeChartResponse> result = reportService.getSiteTypeChartWithoutSerial(status);
        ApiResponse<List<SiteTypeChartResponse>> response = ApiResponse.<List<SiteTypeChartResponse>>builder()
                .message("Get site type chart successfully")
                .data(result)
                .build();
        return ResponseEntity.ok(response);
    }
}
