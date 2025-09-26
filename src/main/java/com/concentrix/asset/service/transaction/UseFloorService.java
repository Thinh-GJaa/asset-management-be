package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateUseFloorRequest;
import com.concentrix.asset.dto.response.UseFloorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface UseFloorService {
    UseFloorResponse getUseFloorById(Integer useFloorId);

    UseFloorResponse createUseFloor(CreateUseFloorRequest request);

    Page<UseFloorResponse> filterUseFloors(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);
}