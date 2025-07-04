package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreatePORequest;
import com.concentrix.asset.dto.response.POResponse;
import com.concentrix.asset.entity.PurchaseOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = POMapperHelper.class)
public interface POMapper {

    @Mapping(target = "vendor", source = "vendorId", qualifiedByName = "vendorIdToVendor")
    @Mapping(target = "warehouse", source = "warehouseId", qualifiedByName = "warehouseIdToWarehouse")
    PurchaseOrder toPurchaseOrder(CreatePORequest request);

    @Mapping(target = "vendor", source = "vendor", qualifiedByName = "mapVendor")
    @Mapping(target = "warehouse", source = "warehouse", qualifiedByName = "mapWarehouse")
    @Mapping(target = "createdBy", source = "createdBy", qualifiedByName = "mapUser")
    @Mapping(target = "poItems", source = "poDetails", qualifiedByName = "mapPOItems")
    POResponse toPOResponse(PurchaseOrder purchaseOrder);

}