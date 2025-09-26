package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateUserRequest;
import com.concentrix.asset.dto.request.UpdateUserRequest;
import com.concentrix.asset.dto.request.UserImportRequest;
import com.concentrix.asset.dto.response.UserResponse;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.mapper.helper.IdMapperHelper;
import com.concentrix.asset.mapper.helper.PasswordHelper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses ={PasswordHelper.class, IdMapperHelper.class})
public interface UserMapper {

    @Mapping(target = "account", source = "account")
    @Mapping(target = "site", source = "site")
    UserResponse toUserResponse(User user);

    @Mapping(target = "role", defaultValue = "OTHER")
    @Mapping(target = "account", source = "accountId", qualifiedByName = "accountIdToAccount")
    User toUser(CreateUserRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "site", source = "siteId", qualifiedByName = "siteIdToSite")
    User updateUser(@MappingTarget User user, UpdateUserRequest request);

    @Mapping(target = "account", source = "msaClient", qualifiedByName = "msaClientToAccount")
    @Mapping(target = "role", defaultValue = "OTHER")
    User toUserWD(UserImportRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "account", source = "msaClient", qualifiedByName = "msaClientToAccount")
    User updateUser(@MappingTarget User user, UserImportRequest request);

}