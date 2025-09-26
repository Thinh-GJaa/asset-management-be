package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateRepairRequest;
import com.concentrix.asset.dto.response.RepairResponse;
import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.mapper.helper.TransactionMapperHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {TransactionMapperHelper.class})
public interface RepairMapper {

    @Mapping(target = "transactionType", expression = "java(com.concentrix.asset.enums.TransactionType.REPAIR)")
    @Mapping(target = "fromWarehouse", source = "fromWarehouseId", qualifiedByName = "warehouseIdToWarehouse")
    AssetTransaction toAssetTransaction(CreateRepairRequest request);

    @Mappings({
            @Mapping(target = "fromWarehouse", source = "fromWarehouse"),
            @Mapping(target = "items", source = "details", qualifiedByName = "mapItems"),
            @Mapping(target = "createdBy", source = "createdBy"),
    })
    RepairResponse toRepairResponse(AssetTransaction transaction);

}