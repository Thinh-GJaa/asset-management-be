package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateTransferRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.concentrix.asset.dto.response.TransferResponse;
import com.concentrix.asset.service.TransferService;
import org.springframework.http.ResponseEntity;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/transaction/transfer-site")
public class TransferController {

        TransferService transferService;

        @PostMapping
        public ResponseEntity<ApiResponse<TransferResponse>> createTransfer(
                        @Valid @RequestBody CreateTransferRequest request) {

                ApiResponse<TransferResponse> apiResponse = ApiResponse.<TransferResponse>builder()
                                .message("Transaction transfer site created successful")
                                .data(transferService.createTransfer(request))
                                .build();

                return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);

        }

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<TransferResponse>> getById(
                        @PathVariable Integer id) {

                ApiResponse<TransferResponse> apiResponse = ApiResponse.<TransferResponse>builder()
                                .message("Get transaction transfer site successful")
                                .data(transferService.getTransferById(id))
                                .build();

                return ResponseEntity.ok(apiResponse);
        }

        @GetMapping("/filter")
        public ResponseEntity<ApiResponse<Page<TransferResponse>>> filterTransfer(
                        @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
                ApiResponse<Page<TransferResponse>> apiResponse = ApiResponse.<Page<TransferResponse>>builder()
                                .message("Filter transaction transfer site successful")
                                .data(transferService.filterTransfers(pageable))
                                .build();

                return ResponseEntity.ok(apiResponse);
        }

}