package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateTransferRequest;
import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.dto.response.TransferResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = { TransactionMapperHelper.class })
public interface TransferMapper {

    @Mapping(target = "transactionType", constant = "TRANSFER")
    @Mapping(target = "toWarehouse", source = "toWarehouseId", qualifiedByName = "warehouseIdToWarehouse")
    @Mapping(target = "fromWarehouse", source = "fromWarehouseId", qualifiedByName = "warehouseIdToWarehouse")
    AssetTransaction toAssetTransaction(CreateTransferRequest request);

    @Mappings({
            @Mapping(target = "toWarehouse", source = "toWarehouse"),
            @Mapping(target = "fromWarehouse", source = "fromWarehouse"),
            @Mapping(target = "createdBy", source = "createdBy"),
            @Mapping(target = "items", source = "details", qualifiedByName = "mapItems")
    })
    TransferResponse toTransferResponse(AssetTransaction transaction);


}
