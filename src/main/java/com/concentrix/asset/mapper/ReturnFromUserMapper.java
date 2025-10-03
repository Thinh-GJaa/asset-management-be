package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateReturnFromUserRequest;
import com.concentrix.asset.dto.response.ReturnFromUserResponse;
import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.mapper.helper.TransactionMapperHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {TransactionMapperHelper.class})
public interface ReturnFromUserMapper {
    @Mapping(target = "transactionType", expression = "java(com.concentrix.asset.enums.TransactionType.RETURN_FROM_USER)")
    @Mapping(target = "toWarehouse", source = "toWarehouseId", qualifiedByName = "warehouseIdToWarehouse")
    @Mapping(target = "userUse.eid", source = "eid")
    AssetTransaction toAssetTransaction(CreateReturnFromUserRequest request);

    @Mappings({
            @Mapping(target = "toWarehouse", source = "toWarehouse"),
            @Mapping(target = "items", source = "details", qualifiedByName = "mapItems"),
            @Mapping(target = "createdBy", source = "createdBy"),
            @Mapping(target = "userUse", source = "userUse"),
            @Mapping(target = "images", source = "images", qualifiedByName = "mapImages")
    })
    ReturnFromUserResponse toReturnFromUserResponse(AssetTransaction transaction);
}