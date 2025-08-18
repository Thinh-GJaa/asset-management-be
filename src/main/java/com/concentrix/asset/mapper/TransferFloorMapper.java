package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateTransferFloorRequest;
import com.concentrix.asset.dto.response.TransferFloorResponse;
import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.mapper.helper.TransactionMapperHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = { TransactionMapperHelper.class })
public interface TransferFloorMapper {
    @Mapping(target = "transactionType", expression = "java(com.concentrix.asset.enums.TransactionType.TRANSFER_FLOOR)")
    @Mapping(target = "fromFloor", source = "fromFloorId", qualifiedByName = "floorIdToFloor")
    @Mapping(target = "toFloor", source = "toFloorId", qualifiedByName = "floorIdToFloor")
    AssetTransaction toAssetTransaction(CreateTransferFloorRequest request);

    @Mappings({
            @Mapping(target = "fromFloor", source = "fromFloor"),
            @Mapping(target = "toFloor", source = "toFloor"),
            @Mapping(target = "items", source = "details", qualifiedByName = "mapItems"),
            @Mapping(target = "createdBy", source = "createdBy")
    })
    TransferFloorResponse toTransferFloorResponse(AssetTransaction transaction);
}