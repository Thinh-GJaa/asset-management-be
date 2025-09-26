package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateUseFloorRequest;
import com.concentrix.asset.dto.response.UseFloorResponse;
import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.mapper.helper.TransactionMapperHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {TransactionMapperHelper.class})
public interface UseFloorMapper {
    @Mapping(target = "transactionType", expression = "java(com.concentrix.asset.enums.TransactionType.USE_FLOOR)")
    @Mapping(target = "fromWarehouse", source = "fromWarehouseId", qualifiedByName = "warehouseIdToWarehouse")
    @Mapping(target = "toFloor", source = "toFloorId", qualifiedByName = "floorIdToFloor")
    AssetTransaction toAssetTransaction(CreateUseFloorRequest request);

    @Mappings({
            @Mapping(target = "fromWarehouse", source = "fromWarehouse"),
            @Mapping(target = "toFloor", source = "toFloor"),
            @Mapping(target = "items", source = "details", qualifiedByName = "mapItems"),
            @Mapping(target = "createdBy", source = "createdBy")
    })
    UseFloorResponse toUseFloorResponse(AssetTransaction transaction);
}