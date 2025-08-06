package com.concentrix.asset.controller.transaction;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateReturnFromUserRequest;
import com.concentrix.asset.dto.response.AssetHandoverResponse;
import com.concentrix.asset.dto.response.ReturnFromUserResponse;
import com.concentrix.asset.service.transaction.ReturnFromUserService;
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

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/transaction/return-from-user")
public class ReturnFromUserController {
    ReturnFromUserService returnFromUserService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReturnFromUserResponse>> createReturnFromUser(
            @Valid @RequestBody CreateReturnFromUserRequest request) {
        ApiResponse<ReturnFromUserResponse> apiResponse = ApiResponse.<ReturnFromUserResponse>builder()
                .message("Transaction return from user created successful")
                .data(returnFromUserService.createReturnFromUser(request))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReturnFromUserResponse>> getById(
            @PathVariable Integer id) {
        ApiResponse<ReturnFromUserResponse> apiResponse = ApiResponse.<ReturnFromUserResponse>builder()
                .message("Get transaction return from user successful")
                .data(returnFromUserService.getReturnFromUserById(id))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<ReturnFromUserResponse>>> filterReturnFromUser(
            @RequestParam(required = false) Integer transactionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ApiResponse<Page<ReturnFromUserResponse>> apiResponse = ApiResponse.<Page<ReturnFromUserResponse>>builder()
                .message("Filter transaction return from user successful")
                .data(returnFromUserService.filterReturnFromUsers(transactionId, fromDate, toDate, pageable))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{id}/handover-form")
    public ResponseEntity<ApiResponse<AssetHandoverResponse>> getHandoverFormById(
            @PathVariable Integer id) {
        ApiResponse<AssetHandoverResponse> apiResponse = ApiResponse.<AssetHandoverResponse>builder()
                .message("Get handover form for return from user successful")
                .data(returnFromUserService.getAssetHandoverForm(id))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

}