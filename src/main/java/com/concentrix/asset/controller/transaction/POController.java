package com.concentrix.asset.controller.transaction;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreatePORequest;
import com.concentrix.asset.dto.response.POResponse;
import com.concentrix.asset.service.transaction.POService;
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
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/purchase-order")
public class POController {

    POService poService;

    @PostMapping
    public ResponseEntity<ApiResponse<POResponse>> createResponse(
            @Valid @RequestBody CreatePORequest createPORequest) {

        ApiResponse<POResponse> response = ApiResponse.<POResponse>builder()
                .message("Purchase order created successfully")
                .data(poService.createPO(createPORequest))
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{poId}")
    public ResponseEntity<ApiResponse<POResponse>> getPOById(@PathVariable String poId) {
        ApiResponse<POResponse> response = ApiResponse.<POResponse>builder()
                .message("Get purchase order successfully")
                .data(poService.getPOById(poId))
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<POResponse>>> filterPO(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate toDate,
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        ApiResponse<Page<POResponse>> response = ApiResponse.<Page<POResponse>>builder()
                .message("Get PO report successfully")
                .data(poService.filterPO(search, fromDate, toDate, pageable))
                .build();
        return ResponseEntity.ok(response);
    }

//    @PatchMapping
//    public ResponseEntity<ApiResponse<SiteResponse>> updateSite(@Valid @RequestBody UpdateSiteRequest request) {
//        ApiResponse<SiteResponse> response = ApiResponse.<SiteResponse>builder()
//                .message("Update site successfully")
//                .data(siteService.updateSite(request))
//                .build();
//        return ResponseEntity.ok(response);
//    }
}
