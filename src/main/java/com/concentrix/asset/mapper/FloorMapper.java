package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateFloorRequest;
import com.concentrix.asset.dto.request.UpdateFloorRequest;
import com.concentrix.asset.dto.response.FloorResponse;
import com.concentrix.asset.entity.Floor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {WarehouseMapperHelper.class})
public interface FloorMapper {

    @Mapping(target = "site", source = "siteId", qualifiedByName = "siteIdToSite")
    Floor toFloor(CreateFloorRequest request);

    @Mapping(target = "site", source = "site")
    FloorResponse toFloorResponse(Floor floor);

    void updateFloor(@MappingTarget Floor floor, UpdateFloorRequest request);
}