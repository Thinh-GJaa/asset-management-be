package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateAccountRequest;
import com.concentrix.asset.dto.request.UpdateAccountRequest;
import com.concentrix.asset.dto.response.AccountResponse;
import com.concentrix.asset.entity.Account;
import com.concentrix.asset.mapper.helper.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {IdMapperHelper.class})
public interface AccountMapper {

    @Mapping(target = "owner", source = "ownerEid", qualifiedByName = "userEidToUser")
    Account toAccount(CreateAccountRequest request);

    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "owner", source = "owner")
    AccountResponse toAccountResponse(Account account);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "owner", source = "ownerEid", qualifiedByName = "userEidToUser")
    void updateAccount(@MappingTarget Account account, UpdateAccountRequest request);

}