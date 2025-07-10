package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateFloorRequest;
import com.concentrix.asset.dto.request.UpdateFloorRequest;
import com.concentrix.asset.dto.response.FloorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FloorService {
    FloorResponse getFloorById(Integer floorId);

    FloorResponse createFloor(CreateFloorRequest request);

    FloorResponse updateFloor(UpdateFloorRequest request);

    Page<FloorResponse> filterFloor(Pageable pageable);
}