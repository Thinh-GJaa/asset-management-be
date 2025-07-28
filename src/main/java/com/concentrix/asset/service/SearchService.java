package com.concentrix.asset.service;

import com.concentrix.asset.dto.response.SearchResultResponse;

public interface SearchService {
    SearchResultResponse search(String query);
}