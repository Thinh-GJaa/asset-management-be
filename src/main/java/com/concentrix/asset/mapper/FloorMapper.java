package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateFloorRequest;
import com.concentrix.asset.dto.request.UpdateFloorRequest;
import com.concentrix.asset.dto.response.FloorResponse;
import com.concentrix.asset.entity.Floor;
import com.concentrix.asset.mapper.helper.IdMapperHelper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {IdMapperHelper.class})
public interface FloorMapper {

    @Mapping(target = "site", source = "siteId", qualifiedByName = "siteIdToSite")
    @Mapping(target = "account", source = "accountId", qualifiedByName = "accountIdToAccount")
    Floor toFloor(CreateFloorRequest request);

    @Mapping(target = "site", source = "site")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "account", source = "account")
    FloorResponse toFloorResponse(Floor floor);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "site", source = "siteId", qualifiedByName = "siteIdToSite")
    @Mapping(target = "account", source = "accountId", qualifiedByName = "accountIdToAccount")
    void updateFloor(@MappingTarget Floor floor, UpdateFloorRequest request);
}