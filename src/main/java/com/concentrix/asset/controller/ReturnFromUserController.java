package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateReturnFromUserRequest;
import com.concentrix.asset.dto.response.ReturnFromUserResponse;
import com.concentrix.asset.service.ReturnFromUserService;
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
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ApiResponse<Page<ReturnFromUserResponse>> apiResponse = ApiResponse.<Page<ReturnFromUserResponse>>builder()
                .message("Filter transaction return from user successful")
                .data(returnFromUserService.filterReturnFromUsers(pageable))
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}