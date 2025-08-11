package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateEWasteRequest;
import com.concentrix.asset.dto.response.EWasteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface EWasteService {
    EWasteResponse getEWasteById(Integer ewasteId);

    EWasteResponse createEWaste(CreateEWasteRequest request);

    Page<EWasteResponse> filterEWastes(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);
}