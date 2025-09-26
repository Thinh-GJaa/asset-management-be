package com.concentrix.asset.controller.transaction;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateDisposalRequest;
import com.concentrix.asset.dto.response.DisposalResponse;
import com.concentrix.asset.service.transaction.DisposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/transaction/disposal")
public class DisposalController {
    DisposalService disposalService;

    @PostMapping
    public ResponseEntity<ApiResponse<DisposalResponse>> createDisposal(
            @Valid @RequestBody CreateDisposalRequest request) {
        ApiResponse<DisposalResponse> apiResponse = ApiResponse.<DisposalResponse>builder()
                .message("Transaction disposal created successful")
                .data(disposalService.createDisposal(request))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DisposalResponse>> getById(
            @PathVariable Integer id) {
        ApiResponse<DisposalResponse> apiResponse = ApiResponse.<DisposalResponse>builder()
                .message("Get transaction disposal successful")
                .data(disposalService.getDisposalById(id))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<DisposalResponse>>> filterDisposal(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate toDate,
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ApiResponse<Page<DisposalResponse>> apiResponse = ApiResponse.<Page<DisposalResponse>>builder()
                .message("Filter transaction disposal successful")
                .data(disposalService.filterDisposals(search, fromDate, toDate, pageable))
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}