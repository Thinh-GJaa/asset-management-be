package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateFloorRequest;
import com.concentrix.asset.dto.request.UpdateFloorRequest;
import com.concentrix.asset.dto.response.FloorResponse;
import com.concentrix.asset.entity.Floor;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.FloorMapper;
import com.concentrix.asset.repository.FloorRepository;
import com.concentrix.asset.service.FloorService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class FloorServiceImpl implements FloorService {
    FloorRepository floorRepository;
    FloorMapper floorMapper;

    @Override
    public FloorResponse getFloorById(Integer floorId) {
        Floor floor = floorRepository.findById(floorId)
                .orElseThrow(() -> new CustomException(ErrorCode.FLOOR_NOT_FOUND, floorId));
        return floorMapper.toFloorResponse(floor);
    }

    @Override
    public FloorResponse createFloor(CreateFloorRequest request) {
        Floor floor = floorMapper.toFloor(request);
        floor = floorRepository.save(floor);
        return floorMapper.toFloorResponse(floor);
    }

    @Override
    public FloorResponse updateFloor(UpdateFloorRequest request) {
        Floor floor = floorRepository.findById(request.getFloorId())
                .orElseThrow(() -> new CustomException(ErrorCode.FLOOR_NOT_FOUND, request.getFloorId()));
        floorMapper.updateFloor(floor, request);
        floor = floorRepository.save(floor);
        return floorMapper.toFloorResponse(floor);
    }

    @Override
    public Page<FloorResponse> filterFloor(Pageable pageable) {
        return floorRepository.findAll(pageable).map(floorMapper::toFloorResponse);
    }
}