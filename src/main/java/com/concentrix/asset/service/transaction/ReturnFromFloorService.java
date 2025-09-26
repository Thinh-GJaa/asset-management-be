package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateReturnFromFloorRequest;
import com.concentrix.asset.dto.response.ReturnFromFloorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ReturnFromFloorService {
    ReturnFromFloorResponse getReturnFromFloorById(Integer returnFromFloorId);

    ReturnFromFloorResponse createReturnFromFloor(CreateReturnFromFloorRequest request);

    Page<ReturnFromFloorResponse> filterReturnFromFloors(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);
}