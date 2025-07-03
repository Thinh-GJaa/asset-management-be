package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateVendorRequest;
import com.concentrix.asset.dto.request.UpdateVendorRequest;
import com.concentrix.asset.dto.response.VendorResponse;
import com.concentrix.asset.service.VendorService;
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
@RequestMapping("/vendor")
public class VendorController {
    VendorService vendorService;

    @PostMapping
    public ResponseEntity<ApiResponse<VendorResponse>> createVendor(@Valid @RequestBody CreateVendorRequest request) {
        ApiResponse<VendorResponse> response = ApiResponse.<VendorResponse>builder()
                .message("Vendor created successfully")
                .data(vendorService.createVendor(request))
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorResponse>> getVendorById(@PathVariable Integer id) {
        ApiResponse<VendorResponse> response = ApiResponse.<VendorResponse>builder()
                .message("Get vendor successfully")
                .data(vendorService.getVendorById(id))
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<VendorResponse>>> filterVendor(
            @PageableDefault(size = 10, page = 0, sort = "vendorId", direction = Sort.Direction.DESC) Pageable pageable) {
        ApiResponse<Page<VendorResponse>> response = ApiResponse.<Page<VendorResponse>>builder()
                .message("Get all vendors successfully")
                .data(vendorService.filterVendor(pageable))
                .build();
        return ResponseEntity.ok(response);
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<VendorResponse>> updateVendor(@Valid @RequestBody UpdateVendorRequest request) {
        ApiResponse<VendorResponse> response = ApiResponse.<VendorResponse>builder()
                .message("Update vendor successfully")
                .data(vendorService.updateVendor(request))
                .build();
        return ResponseEntity.ok(response);
    }
}