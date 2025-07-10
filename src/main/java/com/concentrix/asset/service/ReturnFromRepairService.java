package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateReturnFromRepairRequest;
import com.concentrix.asset.dto.response.ReturnFromRepairResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReturnFromRepairService {
    ReturnFromRepairResponse getReturnFromRepairById(Integer returnId);

    ReturnFromRepairResponse createReturnFromRepair(CreateReturnFromRepairRequest request);

    Page<ReturnFromRepairResponse> filterReturnFromRepairs(Pageable pageable);
}