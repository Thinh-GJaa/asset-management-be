package com.concentrix.asset.controller.transaction;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateReturnFromFloorRequest;
import com.concentrix.asset.dto.response.ReturnFromFloorResponse;
import com.concentrix.asset.service.transaction.ReturnFromFloorService;
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
@RequestMapping("/transaction/return-from-floor")
public class ReturnFromFloorController {
    ReturnFromFloorService returnFromFloorService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReturnFromFloorResponse>> createReturnFromFloor(
            @Valid @RequestBody CreateReturnFromFloorRequest request) {
        ApiResponse<ReturnFromFloorResponse> apiResponse = ApiResponse.<ReturnFromFloorResponse>builder()
                .message("Transaction return from floor created successful")
                .data(returnFromFloorService.createReturnFromFloor(request))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReturnFromFloorResponse>> getById(
            @PathVariable Integer id) {
        ApiResponse<ReturnFromFloorResponse> apiResponse = ApiResponse.<ReturnFromFloorResponse>builder()
                .message("Get transaction return from floor successful")
                .data(returnFromFloorService.getReturnFromFloorById(id))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<ReturnFromFloorResponse>>> filterReturnFromFloor(
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ApiResponse<Page<ReturnFromFloorResponse>> apiResponse = ApiResponse.<Page<ReturnFromFloorResponse>>builder()
                .message("Filter transaction return from floor successful")
                .data(returnFromFloorService.filterReturnFromFloors(pageable))
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}