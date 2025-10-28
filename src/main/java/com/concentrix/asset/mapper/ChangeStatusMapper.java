package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.response.ChangeStatusResponse;
import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.mapper.helper.IdMapperHelper;
import com.concentrix.asset.mapper.helper.TransactionMapperHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = { TransactionMapperHelper.class, IdMapperHelper.class })
public interface ChangeStatusMapper {

    @Mappings( value = {
        @Mapping(target = "createdBy", source = "createdBy"),
        @Mapping(target = "items", source = "details", qualifiedByName = "mapItems")
    })
    ChangeStatusResponse toChangeStatusResponse(AssetTransaction transaction);



}

