package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateReturnFromRepairRequest;
import com.concentrix.asset.dto.response.ReturnFromRepairResponse;
import com.concentrix.asset.entity.AssetTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = { TransactionMapperHelper.class })
public interface ReturnFromRepairMapper {
    @Mapping(target = "transactionType", expression = "java(com.concentrix.asset.enums.TransactionType.RETURN_FROM_REPAIR)")
    @Mapping(target = "toWarehouse", source = "toWarehouseId", qualifiedByName = "warehouseIdToWarehouse")
    AssetTransaction toAssetTransaction(CreateReturnFromRepairRequest request);

    @Mappings({
            @Mapping(target = "toWarehouse", source = "toWarehouse"),
            @Mapping(target = "items", source = "details", qualifiedByName = "mapItems"),
            @Mapping(target = "createdBy", source = "createdBy")
    })
    ReturnFromRepairResponse toReturnFromRepairResponse(AssetTransaction transaction);
}