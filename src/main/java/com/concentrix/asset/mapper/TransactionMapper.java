package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.response.TransactionItemsResponse;
import com.concentrix.asset.dto.response.TransactionResponse;
import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.entity.TransactionDetail;
import com.concentrix.asset.mapper.helper.DeviceMapperHelper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = DeviceMapperHelper.class)
public interface TransactionMapper {
    @Mapping(target = "fromWarehouse", source = "fromWarehouse.warehouseName")
    @Mapping(target = "toWarehouse", source = "toWarehouse.warehouseName")
    @Mapping(target = "createdBy", source = "createdBy.fullName")
    TransactionResponse toTransactionResponse(AssetTransaction transaction);

    @Mapping(target = "deviceName", source = "device.deviceName")
    @Mapping(target = "serialNumber", source = "device.serialNumber")
    @Mapping(target = "quantity", source = "quantity")
    TransactionItemsResponse toTransactionItemsResponse(TransactionDetail detail);

}
