package com.concentrix.asset.controller.transaction;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateReturnFromRepairRequest;
import com.concentrix.asset.dto.response.ReturnFromRepairResponse;
import com.concentrix.asset.service.transaction.ReturnFromRepairService;
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
@RequestMapping("/transaction/return-from-repair")
public class ReturnFromRepairController {
    ReturnFromRepairService returnFromRepairService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReturnFromRepairResponse>> createReturnFromRepair(
            @Valid @RequestBody CreateReturnFromRepairRequest request) {
        ApiResponse<ReturnFromRepairResponse> apiResponse = ApiResponse.<ReturnFromRepairResponse>builder()
                .message("Transaction return from repair created successful")
                .data(returnFromRepairService.createReturnFromRepair(request))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReturnFromRepairResponse>> getById(
            @PathVariable Integer id) {
        ApiResponse<ReturnFromRepairResponse> apiResponse = ApiResponse.<ReturnFromRepairResponse>builder()
                .message("Get transaction return from repair successful")
                .data(returnFromRepairService.getReturnFromRepairById(id))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<ReturnFromRepairResponse>>> filterReturnFromRepair(
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ApiResponse<Page<ReturnFromRepairResponse>> apiResponse = ApiResponse.<Page<ReturnFromRepairResponse>>builder()
                .message("Filter transaction return from repair successful")
                .data(returnFromRepairService.filterReturnFromRepairs(pageable))
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}