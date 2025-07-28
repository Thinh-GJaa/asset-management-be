package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateUserRequest;
import com.concentrix.asset.dto.request.UpdateUserRequest;
import com.concentrix.asset.dto.request.UserImportRequest;
import com.concentrix.asset.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface UserService {
    UserResponse getUserById(String eid);

    UserResponse getUserByEmail(String email);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(UpdateUserRequest request);

    void deleteUser(String id);

    Page<UserResponse> filterUser(Pageable pageable);

    Map<String, Object> importUsers(List<UserImportRequest> importRequests);
}