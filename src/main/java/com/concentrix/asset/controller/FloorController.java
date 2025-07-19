package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateFloorRequest;
import com.concentrix.asset.dto.request.UpdateFloorRequest;
import com.concentrix.asset.dto.response.FloorResponse;
import com.concentrix.asset.service.FloorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/floor")
public class FloorController {
    FloorService floorService;

    @PostMapping
    public ResponseEntity<ApiResponse<FloorResponse>> createFloor(
            @Valid @RequestBody CreateFloorRequest request) {
        ApiResponse<FloorResponse> apiResponse = ApiResponse.<FloorResponse>builder()
                .message("Floor created successfully")
                .data(floorService.createFloor(request))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FloorResponse>> getFloorById(@PathVariable Integer id) {
        ApiResponse<FloorResponse> apiResponse = ApiResponse.<FloorResponse>builder()
                .message("Get floor successfully")
                .data(floorService.getFloorById(id))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<FloorResponse>>> filterFloor(
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ApiResponse<Page<FloorResponse>> apiResponse = ApiResponse.<Page<FloorResponse>>builder()
                .message("Get all floors successfully")
                .data(floorService.filterFloor(pageable))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<FloorResponse>> updateFloor(@Valid @RequestBody UpdateFloorRequest request) {
        ApiResponse<FloorResponse> apiResponse = ApiResponse.<FloorResponse>builder()
                .message("Update floor successfully")
                .data(floorService.updateFloor(request))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/site/{siteId}")
    public ResponseEntity<ApiResponse<Page<FloorResponse>>> getFloorsBySiteId(
            @PathVariable Integer siteId,
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ApiResponse<Page<FloorResponse>> apiResponse = ApiResponse.<Page<FloorResponse>>builder()
                .message("Get floors by site ID successfully")
                .data(floorService.getFloorsBySiteId(siteId, pageable))
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}