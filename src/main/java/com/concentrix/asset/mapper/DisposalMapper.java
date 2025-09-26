package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateDisposalRequest;
import com.concentrix.asset.dto.response.DisposalResponse;
import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.mapper.helper.TransactionMapperHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {TransactionMapperHelper.class})
public interface DisposalMapper {
    @Mapping(target = "transactionType", expression = "java(com.concentrix.asset.enums.TransactionType.DISPOSAL)")
    @Mapping(target = "fromWarehouse", source = "fromWarehouseId", qualifiedByName = "warehouseIdToWarehouse")
    AssetTransaction toAssetTransaction(CreateDisposalRequest request);

    @Mappings({
            @Mapping(target = "fromWarehouse", source = "fromWarehouse"),
            @Mapping(target = "items", source = "details", qualifiedByName = "mapItems"),
            @Mapping(target = "createdBy", source = "createdBy")
    })
    DisposalResponse toDisposalResponse(AssetTransaction transaction);
}