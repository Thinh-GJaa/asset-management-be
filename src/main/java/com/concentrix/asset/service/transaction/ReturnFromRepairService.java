package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateReturnFromRepairRequest;
import com.concentrix.asset.dto.response.ReturnFromRepairResponse;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReturnFromRepairService {
    ReturnFromRepairResponse getReturnFromRepairById(Integer returnId);

    ReturnFromRepairResponse createReturnFromRepair(CreateReturnFromRepairRequest request);

    Page<ReturnFromRepairResponse> filterReturnFromRepairs(Integer transactionId, java.time.LocalDateTime fromDate, java.time.LocalDateTime toDate, Pageable pageable);
}