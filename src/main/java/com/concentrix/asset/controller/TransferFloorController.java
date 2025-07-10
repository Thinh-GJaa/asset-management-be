package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateTransferFloorRequest;
import com.concentrix.asset.dto.response.TransferFloorResponse;
import com.concentrix.asset.service.TransferFloorService;
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
@RequestMapping("/transaction/transfer-floor")
public class TransferFloorController {
    TransferFloorService transferFloorService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransferFloorResponse>> createTransferFloor(
            @Valid @RequestBody CreateTransferFloorRequest request) {
        ApiResponse<TransferFloorResponse> apiResponse = ApiResponse.<TransferFloorResponse>builder()
                .message("Transaction transfer floor created successful")
                .data(transferFloorService.createTransferFloor(request))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransferFloorResponse>> getById(
            @PathVariable Integer id) {
        ApiResponse<TransferFloorResponse> apiResponse = ApiResponse.<TransferFloorResponse>builder()
                .message("Get transaction transfer floor successful")
                .data(transferFloorService.getTransferFloorById(id))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<TransferFloorResponse>>> filterTransferFloor(
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ApiResponse<Page<TransferFloorResponse>> apiResponse = ApiResponse.<Page<TransferFloorResponse>>builder()
                .message("Filter transaction transfer floor successful")
                .data(transferFloorService.filterTransferFloors(pageable))
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}