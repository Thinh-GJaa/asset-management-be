package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.response.SearchResultResponse;
import com.concentrix.asset.service.SearchService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Global search controller for Device & User.
 * Supports search by serial, model, device name, email, eid, msa, sso, ...
 * Returns detail if only one result, or a list if multiple.
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchController {
    SearchService searchService;

    /**
     * Search across all devices and users in the system.
     *
     * @param query The search keyword (serial, name, email, eid, ...)
     * @return SearchResultResponse (list or detail)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<SearchResultResponse>> search(@RequestParam("q") String query) {

        ApiResponse<SearchResultResponse> result = ApiResponse.<SearchResultResponse>builder()
                .data(searchService.search(query))
                .message("Search results for query: " + query)
                .build();
        return ResponseEntity.ok(result);
    }
}