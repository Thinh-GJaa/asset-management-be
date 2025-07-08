package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateUseFloorRequest;
import com.concentrix.asset.dto.response.UseFloorResponse;
import com.concentrix.asset.service.UseFloorService;
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
@RequestMapping("/transaction/use-floor")
public class UseFloorController {
    UseFloorService useFloorService;

    @PostMapping
    public ResponseEntity<ApiResponse<UseFloorResponse>> createUseFloor(
            @Valid @RequestBody CreateUseFloorRequest request) {
        ApiResponse<UseFloorResponse> apiResponse = ApiResponse.<UseFloorResponse>builder()
                .message("Transaction use floor created successful")
                .data(useFloorService.createUseFloor(request))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UseFloorResponse>> getById(
            @PathVariable Integer id) {
        ApiResponse<UseFloorResponse> apiResponse = ApiResponse.<UseFloorResponse>builder()
                .message("Get transaction use floor successful")
                .data(useFloorService.getUseFloorById(id))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<UseFloorResponse>>> filterUseFloor(
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ApiResponse<Page<UseFloorResponse>> apiResponse = ApiResponse.<Page<UseFloorResponse>>builder()
                .message("Filter transaction use floor successful")
                .data(useFloorService.filterUseFloors(pageable))
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}