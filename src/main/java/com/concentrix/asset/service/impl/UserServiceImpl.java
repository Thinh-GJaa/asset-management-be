package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateUserRequest;
import com.concentrix.asset.dto.request.UpdateUserRequest;
import com.concentrix.asset.dto.response.UserResponse;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.enums.Role;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.UserMapper;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {
    UserRepository userRepository;
    UserMapper userMapper;

    @Override
    public UserResponse getUserById(String eid) {
        User user = userRepository.findById(eid)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, eid));
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {

        userRepository.findById(request.getEid())
                .ifPresent(user -> {
                    throw new CustomException(ErrorCode.USER_ALREADY_EXISTS, request.getEid());
                });

        if (userRepository.findBySSO(request.getSSO()).isPresent()) {
            throw new CustomException(ErrorCode.SSO_ALREADY_EXISTS, request.getSSO());
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS, request.getEmail());
        }

        if (userRepository.findByMSA(request.getMSA()).isPresent()) {
            throw new CustomException(ErrorCode.MSA_ALREADY_EXISTS, request.getMSA());
        }
        User user = userMapper.toUser(request);
        user.setRole(Role.IT);
        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse updateUser(UpdateUserRequest request) {

        var context = SecurityContextHolder.getContext();
        String EID = context.getAuthentication().getName();

        log.info("[UserServiceImpl] Updating user with EID: {}", EID);

        User user = userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));

        Optional<User> existingEmail = userRepository.findByEmail(request.getEmail());
        if (existingEmail.isPresent() && !existingEmail.get().getEid().equals(user.getEid())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS, request.getEmail());
        }

        Optional<User> existingSSO = userRepository.findBySSO(request.getSSO());
        if (existingSSO.isPresent() && !existingSSO.get().getEid().equals(user.getEid())) {
            throw new CustomException(ErrorCode.SSO_ALREADY_EXISTS, request.getSSO());
        }

        Optional<User> existingMSA = userRepository.findByMSA(request.getMSA());
        if (existingMSA.isPresent() && !existingMSA.get().getMSA().equals(user.getMSA())) {
            throw new CustomException(ErrorCode.MSA_ALREADY_EXISTS, request.getSSO());
        }

        user = userMapper.updateUser(user, request);
        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Override
    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }

    @Override
    public Page<UserResponse> filterUser( Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        return userPage.map(userMapper::toUserResponse);
    }
}
