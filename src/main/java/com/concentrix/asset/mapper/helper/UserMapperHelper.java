package com.concentrix.asset.mapper.helper;

import com.concentrix.asset.entity.User;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class UserMapperHelper {

    UserRepository userRepository;
    UserService userService;

    @Named("ownerEidToOwner")
    public User getOwnerByEid(String ownerEid) {
        if (ownerEid == null || ownerEid.isBlank()) {
            return null;
        }
        return userRepository.findById(ownerEid)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, ownerEid));
    }

    @Named("getCurrentUser")
    public User getCurrentUser() {
        return userService.getCurrentUser();
    }
}
