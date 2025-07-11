package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.UpdateDeviceRequest;
import com.concentrix.asset.dto.response.DeviceResponse;
import com.concentrix.asset.entity.Device;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = DeviceMapperHelper.class)
public interface DeviceMapper {


    @Mapping(target = "model", source = "model", qualifiedByName = "mapModel")
    @Mapping(target = "poId", source = "deviceId", qualifiedByName = "mapPoId")
    @Mapping(target = "purchaseDate", source = "deviceId", qualifiedByName = "mapPurchaseDate")
    @Mapping(target = "user", source = "currentUser")
    @Mapping(target = "floor", source = "currentFloor")
    @Mapping(target = "warehouse", source = "currentWarehouse")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    DeviceResponse toDeviceResponse(Device device);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)

    Device updateDevice(@MappingTarget Device device, UpdateDeviceRequest request);

}
