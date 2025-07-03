package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateSiteRequest;
import com.concentrix.asset.dto.request.CreateWarehouseRequest;
import com.concentrix.asset.dto.request.UpdateSiteRequest;
import com.concentrix.asset.dto.request.UpdateWarehouseRequest;
import com.concentrix.asset.dto.response.SiteResponse;
import com.concentrix.asset.dto.response.WarehouseResponse;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.entity.Warehouse;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.SiteMapper;
import com.concentrix.asset.mapper.WarehouseMapper;
import com.concentrix.asset.repository.SiteRepository;
import com.concentrix.asset.repository.WarehouseRepository;
import com.concentrix.asset.service.SiteService;
import com.concentrix.asset.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class WarehouseServiceImpl implements WarehouseService {

    WarehouseRepository warehouseRepository;
    WarehouseMapper warehouseMapper;


    @Override
    public WarehouseResponse getWarehouseById(Integer id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.WAREHOUSE_NOT_FOUND, id));

        return warehouseMapper.toWarehouseResponse(warehouse);
    }

    @Override
    public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {

        Warehouse warehouse = warehouseMapper.toWarehouse(request);

        warehouse  = warehouseRepository.save(warehouse);

        return warehouseMapper.toWarehouseResponse(warehouse);
    }

    @Override
    public WarehouseResponse updateWarehouse(UpdateWarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new CustomException(ErrorCode.WAREHOUSE_NOT_FOUND, request.getWarehouseId()));

        warehouseMapper.updateWarehouse(warehouse, request);

        warehouse = warehouseRepository.save(warehouse);

        return warehouseMapper.toWarehouseResponse(warehouse);
    }

    @Override
    public Page<WarehouseResponse> filterWarehouse(Pageable pageable) {
        Page<Warehouse> warehouses = warehouseRepository.findAll(pageable);
        return warehouses.map(warehouseMapper::toWarehouseResponse);
    }
}
