package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateWarehouseRequest;
import com.concentrix.asset.dto.request.UpdateWarehouseRequest;
import com.concentrix.asset.dto.response.WarehouseResponse;
import com.concentrix.asset.entity.Warehouse;
import com.concentrix.asset.mapper.helper.IdMapperHelper;
import org.mapstruct.*;

@Mapper(componentModel = "spring" , uses = IdMapperHelper.class)
public interface WarehouseMapper {

    WarehouseResponse toWarehouseResponse(Warehouse warehouse);

    @Mapping(target = "site", source = "siteId", qualifiedByName = "siteIdToSite")
    Warehouse toWarehouse(CreateWarehouseRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateWarehouse(@MappingTarget Warehouse warehouse, UpdateWarehouseRequest request);
}
