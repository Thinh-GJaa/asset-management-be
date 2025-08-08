package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.UpdateDeviceRequest;
import com.concentrix.asset.dto.response.DeviceMovementHistoryResponse;
import com.concentrix.asset.dto.response.DeviceResponse;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.enums.Role;
import com.concentrix.asset.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/role")
public class RoleController {

    @GetMapping
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        List<Role> roles = Arrays.asList(Role.values());
        ApiResponse<List<Role>> response = ApiResponse.<List<Role>>builder()
                .message("Get all roles successfully")
                .data(roles)
                .build();
        return ResponseEntity.ok(response);
    }

}