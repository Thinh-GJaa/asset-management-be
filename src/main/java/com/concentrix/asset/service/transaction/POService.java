package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreatePORequest;
import com.concentrix.asset.dto.response.POResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface POService {

    POResponse createPO(CreatePORequest createPORequest);

    POResponse getPOById(String poId);

    Page<POResponse> filterPO(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);

}