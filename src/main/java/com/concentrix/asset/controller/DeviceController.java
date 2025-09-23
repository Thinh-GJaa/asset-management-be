package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.UpdateDeviceRequest;
import com.concentrix.asset.dto.request.UpdateSeatNumberRequest;
import com.concentrix.asset.dto.response.DeviceResponse;
import com.concentrix.asset.dto.response.DeviceMovementHistoryResponse;
import com.concentrix.asset.dto.response.DeviceWithoutSerialSummaryResponse;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/device")
public class DeviceController {

        DeviceService deviceService;

        @GetMapping("/{deviceId}")
        public ResponseEntity<ApiResponse<DeviceResponse>> getDeviceById(@PathVariable Integer deviceId) {
                ApiResponse<DeviceResponse> response = ApiResponse.<DeviceResponse>builder()
                                .message("Get device successfully")
                                .data(deviceService.getDeviceById(deviceId))
                                .build();
                return ResponseEntity.ok(response);
        }

        @GetMapping("/filter")
        public ResponseEntity<ApiResponse<Page<DeviceResponse>>> filterDevice(
                        @PageableDefault(size = 10, page = 0, sort = "deviceName", direction = Sort.Direction.DESC) Pageable pageable,
                        @RequestParam(value = "modelId", required = false) Integer modelId,
                        @RequestParam(value = "search", required = false) String search,
                        @RequestParam(value = "status", required = false) DeviceStatus status,
                        @RequestParam(value = "type", required = false) DeviceType type) {
                ApiResponse<Page<DeviceResponse>> response = ApiResponse.<Page<DeviceResponse>>builder()
                                .message("Filter device successfully")
                                .data(deviceService.filterDevices(search, type, modelId, status, pageable))
                                .build();
                return ResponseEntity.ok(response);
        }

        @GetMapping("/types")
        public ResponseEntity<ApiResponse<List<String>>> getDeviceTypes() {
                List<String> types = deviceService.getAllDeviceTypes();
                ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
                                .message("Get device types successfully")
                                .data(types)
                                .build();
                return ResponseEntity.ok(response);
        }

        @GetMapping("/status")
        public ResponseEntity<ApiResponse<List<String>>> getDeviceStatuses() {
                List<String> status = deviceService.getDeviceStatuses();
                ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
                                .message("Get device statuses successfully")
                                .data(status)
                                .build();
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{serialNumber}/history")
        public ResponseEntity<ApiResponse<List<DeviceMovementHistoryResponse>>> getDeviceMovementHistory(
                        @PathVariable String serialNumber) {
                ApiResponse<List<DeviceMovementHistoryResponse>> response = ApiResponse
                                .<List<DeviceMovementHistoryResponse>>builder()
                                .message("Get device movement history successfully")
                                .data(deviceService.getDeviceMovementHistoryBySerial(serialNumber))
                                .build();
                return ResponseEntity.ok(response);
        }

    @GetMapping("/serial/{serialNumber}")
    public ResponseEntity<ApiResponse<Boolean>> isDeviceBySerialWithoutInStock(
            @PathVariable String serialNumber) {
        ApiResponse<Boolean> response = ApiResponse
                .<Boolean>builder()
                .message("Is device in stock")
                .data(deviceService.isDeviceBySerialInStock(serialNumber))
                .build();
        return ResponseEntity.ok(response);
    }

        @PatchMapping
        public ResponseEntity<ApiResponse<DeviceResponse>> updateDevice(
                        @Valid @RequestBody UpdateDeviceRequest request) {
                ApiResponse<DeviceResponse> response = ApiResponse.<DeviceResponse>builder()
                                .message("Update device successfully")
                                .data(deviceService.updateDevice(request))
                                .build();
                return ResponseEntity.ok(response);
        }

        @PatchMapping("/seat-number")
        public ResponseEntity<ApiResponse<Void>> updateSeatNumber(
                        @Valid @RequestBody List<UpdateSeatNumberRequest> request) {

                deviceService.updateSeatNumber(request);

                ApiResponse<Void> response = ApiResponse.<Void>builder()
                                .message("Update seat number successfully")
                                .build();
                return ResponseEntity.ok(response);
        }

        @GetMapping("/filter/non-seat-number")
        public ResponseEntity<ApiResponse<Page<DeviceResponse>>> filterDeviceNonSeatNumber(
                        @PageableDefault(size = 10, page = 0, sort = "serialNumber", direction = Sort.Direction.DESC) Pageable pageable,
                        @RequestParam(value = "siteId", required = false) Integer siteId,
                        @RequestParam(value = "search", required = false) String search,
                        @RequestParam(value = "floorId", required = false) Integer floorId) {
                ApiResponse<Page<DeviceResponse>> response = ApiResponse.<Page<DeviceResponse>>builder()
                                .message("Filter device successfully")
                                .data(deviceService.filterDevicesNonSeatNumber(search, siteId, floorId, pageable))
                                .build();
                return ResponseEntity.ok(response);
        }
}