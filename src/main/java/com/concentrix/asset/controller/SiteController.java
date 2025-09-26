package com.concentrix.asset.controller;


import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateSiteRequest;
import com.concentrix.asset.dto.request.UpdateSiteRequest;
import com.concentrix.asset.dto.response.SiteResponse;
import com.concentrix.asset.service.SiteService;
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
@RequestMapping("/site")
public class SiteController {
    SiteService siteService;

    @PostMapping
    public ResponseEntity<ApiResponse<SiteResponse>> createSite(
            @Valid @RequestBody CreateSiteRequest createSiteRequest) {

        ApiResponse<SiteResponse> response = ApiResponse.<SiteResponse>builder()
                .message("Site created successfully")
                .data(siteService.createSite(createSiteRequest))
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SiteResponse>> getSiteById(@PathVariable Integer id) {
        ApiResponse<SiteResponse> response = ApiResponse.<SiteResponse>builder()
                .message("Get site successfully")
                .data(siteService.getSiteById(id))
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<SiteResponse>>> filterSite(
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        ApiResponse<Page<SiteResponse>> response = ApiResponse.<Page<SiteResponse>>builder()
                .message("Get all sites successfully")
                .data(siteService.filterSite(pageable))
                .build();
        return ResponseEntity.ok(response);
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<SiteResponse>> updateSite(@Valid @RequestBody UpdateSiteRequest request) {
        ApiResponse<SiteResponse> response = ApiResponse.<SiteResponse>builder()
                .message("Update site successfully")
                .data(siteService.updateSite(request))
                .build();
        return ResponseEntity.ok(response);
    }
}
