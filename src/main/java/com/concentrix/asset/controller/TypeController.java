package com.concentrix.asset.controller;


import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.service.TypeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/type")
public class TypeController {

    TypeService typeService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<DeviceType>>> getTypes() {
        ApiResponse<List<DeviceType>> apiResponse = ApiResponse.<List<DeviceType>>builder()
                .message("Get all device types successfully")
                .data(typeService.getTypes())
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/with-serial")
    public ResponseEntity<ApiResponse<List<DeviceType>>> getTypesWithSerial() {
        ApiResponse<List<DeviceType>> apiResponse = ApiResponse.<List<DeviceType>>builder()
                .message("Get all device types with serial successfully")
                .data(typeService.getTypeWithSerial())
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/without-serial")
    public ResponseEntity<ApiResponse<List<DeviceType>>> getTypesWithoutSerial() {
        ApiResponse<List<DeviceType>> apiResponse = ApiResponse.<List<DeviceType>>builder()
                .message("Get all device types without serial successfully")
                .data(typeService.getTypeWithoutSerial())
                .build();
        return ResponseEntity.ok(apiResponse);
    }


}
