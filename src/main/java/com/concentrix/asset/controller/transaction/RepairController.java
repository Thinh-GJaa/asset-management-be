package com.concentrix.asset.controller.transaction;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateRepairRequest;
import com.concentrix.asset.dto.response.RepairResponse;
import com.concentrix.asset.service.transaction.RepairService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/transaction/repair")
public class RepairController {

    RepairService repairService;

    @PostMapping
    public ResponseEntity<ApiResponse<RepairResponse>> createRepair(
            @Valid @RequestBody CreateRepairRequest request) {
        ApiResponse<RepairResponse> apiResponse = ApiResponse.<RepairResponse>builder()
                .message("Transaction repair created successful")
                .data(repairService.createRepair(request))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RepairResponse>> getById(
            @PathVariable Integer id) {
        ApiResponse<RepairResponse> apiResponse = ApiResponse.<RepairResponse>builder()
                .message("Get transaction repair successful")
                .data(repairService.getRepairById(id))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<RepairResponse>>> filterRepair(
            @RequestParam(required = false) Integer transactionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ApiResponse<Page<RepairResponse>> apiResponse = ApiResponse.<Page<RepairResponse>>builder()
                .message("Filter transaction repair successful")
                .data(repairService.filterRepairs(transactionId, fromDate, toDate, pageable))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

}