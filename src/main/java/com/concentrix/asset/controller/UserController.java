package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateUserRequest;
import com.concentrix.asset.dto.request.UpdateUserRequest;
import com.concentrix.asset.dto.response.UserResponse;
import com.concentrix.asset.dto.response.DeviceBorrowingInfoResponse;
import com.concentrix.asset.service.UserService;
import com.concentrix.asset.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user")
public class UserController {
        UserService userService;
        DeviceService deviceService;

        @PostMapping
        public ResponseEntity<ApiResponse<UserResponse>> createUser(
                        @Valid @RequestBody CreateUserRequest createUserRequest) {
                ApiResponse<UserResponse> response = ApiResponse.<UserResponse>builder()
                                .message("User created successfully")
                                .data(userService.createUser(createUserRequest))
                                .build();
                return new ResponseEntity<>(response, HttpStatus.CREATED);
        }

        @GetMapping("/{eid}")
        public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable String eid) {
                ApiResponse<UserResponse> response = ApiResponse.<UserResponse>builder()
                                .message("Get user successfully")
                                .data(userService.getUserById(eid))
                                .build();
                return ResponseEntity.ok(response);
        }

        @GetMapping("/filter")
        public ResponseEntity<ApiResponse<Page<UserResponse>>> filterUser(
                        @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
                ApiResponse<Page<UserResponse>> response = ApiResponse.<Page<UserResponse>>builder()
                                .message("Get all users successfully")
                                .data(userService.filterUser(pageable))
                                .build();
                return ResponseEntity.ok(response);
        }

        @PatchMapping
        public ResponseEntity<ApiResponse<UserResponse>> updateUser(@Valid @RequestBody UpdateUserRequest request) {
                ApiResponse<UserResponse> response = ApiResponse.<UserResponse>builder()
                                .message("Update user successfully")
                                .data(userService.updateUser(request))
                                .build();
                return ResponseEntity.ok(response);
        }

        @GetMapping("/borrowing-devices")
        public ResponseEntity<ApiResponse<List<DeviceBorrowingInfoResponse>>> getAllUserBorrowingDevices() {
                ApiResponse<List<DeviceBorrowingInfoResponse>> response = ApiResponse
                                .<List<DeviceBorrowingInfoResponse>>builder()
                                .message("Get all user borrowing devices successfully")
                                .data(deviceService.getAllUserBorrowingDevices())
                                .build();
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{eid}/borrowing-devices")
        public ResponseEntity<ApiResponse<List<DeviceBorrowingInfoResponse.DeviceInfo>>> getBorrowingDevicesByUser(
                        @PathVariable String eid) {
                ApiResponse<List<DeviceBorrowingInfoResponse.DeviceInfo>> response = ApiResponse.<List<DeviceBorrowingInfoResponse.DeviceInfo>>builder()
                                .message("Get borrowing devices by user successfully")
                                .data(deviceService.getBorrowingDevicesByUser(eid))
                                .build();
                return ResponseEntity.ok(response);
        }

        @GetMapping("/me")
        public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
                String eid = jwt.getSubject(); // Lấy EID từ claim 'sub'
                ApiResponse<UserResponse> response = ApiResponse.<UserResponse>builder()
                                .message("Get current user profile successfully")
                                .data(userService.getUserById(eid))
                                .build();
                return ResponseEntity.ok(response);
        }
}