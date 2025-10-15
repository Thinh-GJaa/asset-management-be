package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.response.CompareDataResponse;
import com.concentrix.asset.dto.response.DeviceChangesResponse;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.service.SnapshotDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/compare-data")
public class CompareDataController {

    SnapshotDeviceService snapshotDeviceService;

    @GetMapping
    public ResponseEntity<ApiResponse<CompareDataResponse>> getDataCompare(
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam(defaultValue = "site") String groupBy,
            @RequestParam(required = false) Integer siteId,
            @RequestParam(required = false) DeviceStatus status,
            @RequestParam(required = false) DeviceType type) {

        // Parse String to LocalDate
        LocalDate fromLocalDate = LocalDate.parse(fromDate);
        LocalDate toLocalDate = LocalDate.parse(toDate);

        ApiResponse<CompareDataResponse> response = ApiResponse.<CompareDataResponse>builder()
                .message("Get compare data successfully")
                .data(snapshotDeviceService.getDataCompare(fromLocalDate, toLocalDate, groupBy, siteId, status, type))
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/device-changes")
    public ResponseEntity<ApiResponse<DeviceChangesResponse>> getDeviceChanges(
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam(required = false) Integer siteId,
            @RequestParam(required = false) DeviceStatus status,
            @RequestParam(required = false) DeviceType type) {

        // Parse String to LocalDate
        LocalDate fromLocalDate = LocalDate.parse(fromDate);
        LocalDate toLocalDate = LocalDate.parse(toDate);

        ApiResponse<DeviceChangesResponse> response = ApiResponse.<DeviceChangesResponse>builder()
                .message("Get device changes successfully")
                .data(snapshotDeviceService.getDeviceChanges(fromLocalDate, toLocalDate, siteId, status, type))
                .build();
        return ResponseEntity.ok(response);
    }

}