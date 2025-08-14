package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateUserRequest;
import com.concentrix.asset.dto.request.UpdateUserRequest;
import com.concentrix.asset.dto.response.UserResponse;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.mapper.helper.PasswordHelper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = PasswordHelper.class)
public interface UserMapper {

    UserResponse toUserResponse(User user);

    @Mapping(target = "role", defaultValue = "OTHER")
    @Mapping(target = "password", source = "password", qualifiedByName = "hashPassword")
    User toUser(CreateUserRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    User updateUser(@MappingTarget User user, UpdateUserRequest request);
}