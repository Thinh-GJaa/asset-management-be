package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateAssignmentRequest;
import com.concentrix.asset.dto.response.AssetHandoverResponse;
import com.concentrix.asset.dto.response.AssignmentResponse;
import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.mapper.helper.IdMapperHelper;
import com.concentrix.asset.mapper.helper.TransactionMapperHelper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = { TransactionMapperHelper.class, IdMapperHelper.class})
public interface AssignmentMapper {

    @Mapping(target = "transactionType", expression = "java(com.concentrix.asset.enums.TransactionType.ASSIGNMENT)")
    @Mapping(target = "fromWarehouse", source = "fromWarehouseId", qualifiedByName = "warehouseIdToWarehouse")
    @Mapping(target = "userUse", source = "eid", qualifiedByName = "userEidToUser")
    AssetTransaction toAssetTransaction(CreateAssignmentRequest request);

    @Mappings({

            @Mapping(target = "fromWarehouse", source = "fromWarehouse"),
            @Mapping(target = "items", source = "details", qualifiedByName = "mapItems"),
            @Mapping(target = "createdBy", source = "createdBy"),
            @Mapping(target = "userUse", source = "userUse"),
            @Mapping(target = "returnDate", source = "returnDate", dateFormat = "yyyy-MM-dd")
    })

    AssignmentResponse toAssignmentResponse(AssetTransaction transaction);

    @Mappings(value = {
            @Mapping(target = "itPerson", source = "createdBy.fullName"),
            @Mapping(target = "location", source = "assetTransaction", qualifiedByName = "mapLocation"),
            @Mapping(target = "endUser", source = "userUse.fullName"),
            @Mapping(target = "msa", source = "userUse.msa"),
            @Mapping(target = "employeeId", source = "userUse.eid"),
            @Mapping(target = "ssoEmail", source = "userUse.email"),
            @Mapping(target = "assetType", source = "assetTransaction", qualifiedByName = "transactionTypeToAssetType"),
            @Mapping(target = "issueDate", dateFormat = "dd-MM-YYYY", source = "createdAt"),
            @Mapping(target = "role", source = "userUse.jobTitle"),
            @Mapping(target = "returnDate", dateFormat = "dd-MM-YYYY", source = "returnDate"),
            @Mapping(target = "items", source = "details", qualifiedByName = "mapItems"),
    })
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    AssetHandoverResponse toAssetHandoverResponse(AssetTransaction assetTransaction);


}
