package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateReturnFromFloorRequest;
import com.concentrix.asset.dto.response.ReturnFromFloorResponse;
import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.mapper.helper.TransactionMapperHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {TransactionMapperHelper.class})
public interface ReturnFromFloorMapper {
    @Mapping(target = "transactionType", expression = "java(com.concentrix.asset.enums.TransactionType.RETURN_FROM_FLOOR)")
    @Mapping(target = "fromFloor", source = "fromFloorId", qualifiedByName = "floorIdToFloor")
    @Mapping(target = "toWarehouse", source = "toWarehouseId", qualifiedByName = "warehouseIdToWarehouse")
    AssetTransaction toAssetTransaction(CreateReturnFromFloorRequest request);

    @Mappings({
            @Mapping(target = "fromFloor", source = "fromFloor"),
            @Mapping(target = "toWarehouse", source = "toWarehouse"),
            @Mapping(target = "items", source = "details", qualifiedByName = "mapItems"),
            @Mapping(target = "createdBy", source = "createdBy")
    })
    ReturnFromFloorResponse toReturnFromFloorResponse(AssetTransaction transaction);
}