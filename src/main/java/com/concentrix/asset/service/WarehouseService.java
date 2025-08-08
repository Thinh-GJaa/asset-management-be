package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateSiteRequest;
import com.concentrix.asset.dto.request.CreateWarehouseRequest;
import com.concentrix.asset.dto.request.UpdateSiteRequest;
import com.concentrix.asset.dto.request.UpdateWarehouseRequest;
import com.concentrix.asset.dto.response.SiteResponse;
import com.concentrix.asset.dto.response.WarehouseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WarehouseService {

     WarehouseResponse getWarehouseById(Integer id);

     WarehouseResponse createWarehouse(CreateWarehouseRequest request);

     WarehouseResponse updateWarehouse(UpdateWarehouseRequest request);

     Page<WarehouseResponse> filterWarehouse(Pageable pageable, String search, Integer siteId);

}
