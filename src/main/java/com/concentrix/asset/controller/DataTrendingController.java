package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.response.DataTrending;
import com.concentrix.asset.dto.response.DeviceChangeDetail;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.service.SnapshotDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/data-trending")
public class DataTrendingController {

    SnapshotDeviceService snapshotDeviceService;

    /**
     * API lấy dữ liệu trending để vẽ chart
     * Lấy 7 mốc thời gian snapshot gần nhất với filter tùy chọn
     *
     * @param siteId ID của site (optional)
     * @param status Trạng thái device (optional)
     * @param type   Loại device (optional)
     * @return DataTrending chứa labels và datasets cho chart
     */
    @GetMapping("/chart")
    public ResponseEntity<ApiResponse<DataTrending>> getDataTrending(
            @RequestParam(required = false) Integer siteId,
            @RequestParam(required = false) DeviceStatus status,
            @RequestParam(required = false) DeviceType type) {
        ApiResponse<DataTrending> apiResponse = ApiResponse.<DataTrending>builder()
                .message("Get data trending successfully")
                .data(snapshotDeviceService.getDataTrending(siteId, status, type))
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * API lấy chi tiết devices thay đổi giữa 2 mốc thời gian
     *
     * @param fromDate Ngày bắt đầu (format: dd/MM/yyyy)
     * @param toDate   Ngày kết thúc (format: dd/MM/yyyy)
     * @param siteId   ID của site (optional)
     * @param status   Trạng thái device (optional)
     * @param type     Loại device (optional)
     * @return DeviceChangeDetail chứa danh sách devices được thêm và xóa
     */
    @GetMapping("/changes")
    public ResponseEntity<ApiResponse<DeviceChangeDetail>> getDeviceChanges(
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam(required = false) Integer siteId,
            @RequestParam(required = false) DeviceStatus status,
            @RequestParam(required = false) DeviceType type) {
        ApiResponse<DeviceChangeDetail> apiResponse = ApiResponse.<DeviceChangeDetail>builder()
                .message("Get device changes successfully")
                .data(snapshotDeviceService.getDeviceChanges(fromDate, toDate, siteId, status, type))
                .build();

        return ResponseEntity.ok(apiResponse);
    }
}