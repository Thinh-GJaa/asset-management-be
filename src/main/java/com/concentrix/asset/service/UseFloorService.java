package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateUseFloorRequest;
import com.concentrix.asset.dto.response.UseFloorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UseFloorService {
    UseFloorResponse getUseFloorById(Integer useFloorId);

    UseFloorResponse createUseFloor(CreateUseFloorRequest request);

    Page<UseFloorResponse> filterUseFloors(Pageable pageable);
}