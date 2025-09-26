package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.response.DeviceResponse;
import com.concentrix.asset.dto.response.SearchResultResponse;
import com.concentrix.asset.dto.response.UserResponse;
import com.concentrix.asset.entity.Device;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.mapper.DeviceMapper;
import com.concentrix.asset.mapper.UserMapper;
import com.concentrix.asset.repository.DeviceRepository;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class SearchServiceImpl implements SearchService {
    UserRepository userRepository;
    DeviceRepository deviceRepository;
    UserMapper userMapper;
    DeviceMapper deviceMapper;

    /**
     * Global search for both User and Device.
     * - Tìm kiếm theo từ khóa (query) trên các trường: email, eid, fullName, sso, msa (User) và serialNumber (Device).
     * - Trả về danh sách user và device phù hợp, map sang DTO chuẩn cho FE.
     * - Kết quả trả về gồm: tổng số kết quả, danh sách UserResponse, danh sách DeviceResponse.
     *
     * @param query Từ khóa tìm kiếm (có thể là email, eid, serial, tên thiết bị, model, ...)
     * @return SearchResultResponse gồm users và devices phù hợp
     */
    @Override
    public SearchResultResponse search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return SearchResultResponse.builder()
                    .total(0)
                    .users(List.of())
                    .devices(List.of())
                    .build();
        }
        String q = query.trim().toLowerCase();

        // 1. Tìm kiếm user theo các trường: email, eid, fullName, sso, msa
        List<User> users = userRepository.findAll().stream()
                .filter(u -> (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)) ||
                        (u.getEid() != null && u.getEid().toLowerCase().contains(q)) ||
                        (u.getFullName() != null && u.getFullName().toLowerCase().contains(q)) ||
                        (u.getSso() != null && u.getSso().toLowerCase().contains(q)) ||
                        (u.getMsa() != null && u.getMsa().toLowerCase().contains(q)))
                .toList();

        // 2. Tìm kiếm device theo các trường: serialNumber, deviceName, modelName
        List<Device> devices = deviceRepository.findAll().stream()
                .filter(d ->
                        (d.getSerialNumber() != null &&
                                d.getSerialNumber().toLowerCase().contains(q)))
                .toList();

        // 3. Map kết quả sang DTO chuẩn cho FE
        List<UserResponse> userResponses = users.stream()
                .map(userMapper::toUserResponse)
                .toList();

        List<DeviceResponse> deviceResponses = devices.stream()
                .map(deviceMapper::toDeviceResponse)
                .toList();

        // 4. Trả về kết quả tổng hợp
        return SearchResultResponse.builder()
                .total(userResponses.size() + deviceResponses.size())
                .users(userResponses)
                .devices(deviceResponses)
                .build();
    }
}