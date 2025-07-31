package com.concentrix.asset.controller.transaction;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateEWasteRequest;
import com.concentrix.asset.dto.response.EWasteResponse;
import com.concentrix.asset.service.transaction.EWasteService;
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
@RequestMapping("/transaction/ewaste")
public class EWasteController {
    EWasteService ewasteService;

    @PostMapping
    public ResponseEntity<ApiResponse<EWasteResponse>> createEWaste(
            @Valid @RequestBody CreateEWasteRequest request) {
        ApiResponse<EWasteResponse> apiResponse = ApiResponse.<EWasteResponse>builder()
                .message("Transaction ewaste created successful")
                .data(ewasteService.createEWaste(request))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EWasteResponse>> getById(
            @PathVariable Integer id) {
        ApiResponse<EWasteResponse> apiResponse = ApiResponse.<EWasteResponse>builder()
                .message("Get transaction ewaste successful")
                .data(ewasteService.getEWasteById(id))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<EWasteResponse>>> filterEWaste(
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ApiResponse<Page<EWasteResponse>> apiResponse = ApiResponse.<Page<EWasteResponse>>builder()
                .message("Filter transaction ewaste successful")
                .data(ewasteService.filterEWastes(pageable))
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}