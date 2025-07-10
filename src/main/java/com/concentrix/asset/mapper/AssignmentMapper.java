package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateAssignmentRequest;
import com.concentrix.asset.dto.response.AssignmentResponse;
import com.concentrix.asset.entity.AssetTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = { TransactionMapperHelper.class })
public interface AssignmentMapper {

    @Mapping(target = "transactionType", expression = "java(com.concentrix.asset.enums.TransactionType.ASSIGNMENT)")
    @Mapping(target = "fromWarehouse", source = "fromWarehouseId", qualifiedByName = "warehouseIdToWarehouse")
    AssetTransaction toAssetTransaction(CreateAssignmentRequest request);

    @Mappings({

            @Mapping(target = "fromWarehouse", source = "fromWarehouse"),
            @Mapping(target = "items", source = "details", qualifiedByName = "mapItems"),
            @Mapping(target = "createdBy", source = "createdBy"),
            @Mapping(target = "userUse", source = "userUse")
    })
    AssignmentResponse toAssignmentResponse(AssetTransaction transaction);

}
