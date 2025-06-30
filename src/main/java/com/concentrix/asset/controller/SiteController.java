package com.concentrix.asset.controller;


import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateSiteRequest;
import com.concentrix.asset.dto.response.SiteResponse;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.service.SiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
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


}
