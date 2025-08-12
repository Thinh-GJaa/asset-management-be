package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateReturnFromRepairRequest;
import com.concentrix.asset.dto.response.ReturnFromRepairResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReturnFromRepairService {
    ReturnFromRepairResponse getReturnFromRepairById(Integer returnId);

    ReturnFromRepairResponse createReturnFromRepair(CreateReturnFromRepairRequest request);

    Page<ReturnFromRepairResponse> filterReturnFromRepairs(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);
}