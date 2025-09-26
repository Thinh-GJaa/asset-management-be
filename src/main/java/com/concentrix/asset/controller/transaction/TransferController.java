package com.concentrix.asset.controller.transaction;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateTransferRequest;
import com.concentrix.asset.dto.response.TransferResponse;
import com.concentrix.asset.service.transaction.TransferService;
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
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate fromDate,
                        @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate toDate,
                        @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
                ApiResponse<Page<TransferResponse>> apiResponse = ApiResponse.<Page<TransferResponse>>builder()
                                .message("Filter transaction transfer site successful")
                                .data(transferService.filterTransfers(search, fromDate, toDate, pageable))
                                .build();

                return ResponseEntity.ok(apiResponse);
        }

        @GetMapping("/pending")
        public ResponseEntity<ApiResponse<Page<TransferResponse>>> filterTransfersSitePending(
                        @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
                ApiResponse<Page<TransferResponse>> apiResponse = ApiResponse.<Page<TransferResponse>>builder()
                                .message("Filter transaction transfer site pending successful")
                                .data(transferService.filterTransfersSitePending(pageable))
                                .build();

                return ResponseEntity.ok(apiResponse);
        }

        @PostMapping("/{id}/confirm")
        public ResponseEntity<ApiResponse<TransferResponse>> confirmTransfer(@PathVariable Integer id) {

                transferService.confirmTransfer(id);

                ApiResponse<TransferResponse> response = ApiResponse.<TransferResponse>builder()
                                .message("Transfer site confirmed successfully")
                                .build();
                return ResponseEntity.ok(response);
        }

        @PostMapping("/{id}/approve")
        public ResponseEntity<ApiResponse<Void>> approveTransfer(@PathVariable Integer id) {

                transferService.approveTransfer(id);

                ApiResponse<Void> response = ApiResponse.<Void>builder()
                        .message("Transfer site approve successfully")
                        .build();
                return ResponseEntity.ok(response);
        }

        @PostMapping("/approve")
        public ResponseEntity<ApiResponse<Void>> approveTransferByToken(@RequestParam String token) {

                transferService.approveTransferByToken(token);

                ApiResponse<Void> response = ApiResponse.<Void>builder()
                        .message("Transfer site approve successfully")
                        .build();
                return ResponseEntity.ok(response);
        }

}