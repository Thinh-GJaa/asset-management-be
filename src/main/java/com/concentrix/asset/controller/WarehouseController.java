package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateWarehouseRequest;
import com.concentrix.asset.dto.request.UpdateWarehouseRequest;
import com.concentrix.asset.dto.response.WarehouseResponse;
import com.concentrix.asset.service.WarehouseService;
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
@RequestMapping("/warehouse")
public class WarehouseController {
    WarehouseService warehouseService;

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseResponse>> createWarehouse(@Valid @RequestBody CreateWarehouseRequest request) {
        ApiResponse<WarehouseResponse> response = ApiResponse.<WarehouseResponse>builder()
                .message("Warehouse created successfully")
                .data(warehouseService.createWarehouse(request))
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseResponse>> getWarehouseById(@PathVariable Integer id) {
        ApiResponse<WarehouseResponse> response = ApiResponse.<WarehouseResponse>builder()
                .message("Get warehouse successfully")
                .data(warehouseService.getWarehouseById(id))
                .build();
        return ResponseEntity.ok(response);
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<WarehouseResponse>> updateWarehouse(@Valid @RequestBody UpdateWarehouseRequest request) {
        ApiResponse<WarehouseResponse> response = ApiResponse.<WarehouseResponse>builder()
                .message("Update warehouse successfully")
                .data(warehouseService.updateWarehouse(request))
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<WarehouseResponse>>> filterWarehouses(
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "siteId", required = false) Integer siteId
    ) {
        ApiResponse<Page<WarehouseResponse>> response = ApiResponse.<Page<WarehouseResponse>>builder()
                .message("Filter warehouse successfully")
                .data(warehouseService.filterWarehouse(pageable, search, siteId))
                .build();
        return ResponseEntity.ok(response);
    }
}