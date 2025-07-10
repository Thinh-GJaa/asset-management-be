package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.response.*;
import com.concentrix.asset.entity.Device;
import com.concentrix.asset.entity.Model;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.repository.DeviceRepository;
import com.concentrix.asset.repository.ModelRepository;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class SearchServiceImpl implements SearchService {
    UserRepository userRepository;
    DeviceRepository deviceRepository;
    ModelRepository modelRepository;

    @Override
    public SearchResultResponse search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return SearchResultResponse.builder().results(Collections.emptyList()).build();
        }
        String q = query.trim().toLowerCase();
        // Ưu tiên tìm user trước
        List<User> users = userRepository.findAll().stream()
                .filter(u -> (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)) ||
                        (u.getEid() != null && u.getEid().toLowerCase().contains(q)) ||
                        (u.getMSA() != null && u.getMSA().toLowerCase().contains(q)) ||
                        (u.getSSO() != null && u.getSSO().toLowerCase().contains(q)) ||
                        (u.getFullName() != null && u.getFullName().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        if (users.size() == 1) {
            User u = users.get(0);
            return SearchResultResponse.builder()
                    .userDetail(UserDetailResponse.builder()
                            .eid(u.getEid())
                            .fullName(u.getFullName())
                            .email(u.getEmail())
                            .msa(u.getMSA())
                            .sso(u.getSSO())
                            .position(u.getJobTitle())
                            .build())
                    .build();
        } else if (users.size() > 1) {
            List<SearchResultResponse.ResultItem> results = users.stream()
                    .map(u -> SearchResultResponse.ResultItem.builder()
                            .type("USER")
                            .eid(u.getEid())
                            .fullName(u.getFullName())
                            .email(u.getEmail())
                            .msa(u.getMSA())
                            .sso(u.getSSO())
                            .build())
                    .collect(Collectors.toList());
            return SearchResultResponse.builder().results(results).build();
        }
        // Nếu không có user, tìm device
        List<Device> devices = deviceRepository
                .findAll().stream().filter(
                        d -> (d.getSerialNumber() != null && d.getSerialNumber().toLowerCase().contains(q)) ||
                                (d.getDeviceName() != null && d.getDeviceName().toLowerCase().contains(q)) ||
                                (d.getModel() != null && d.getModel().getModelName() != null
                                        && d.getModel().getModelName().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        if (devices.size() == 1) {
            Device d = devices.get(0);
            Model m = d.getModel();
            return SearchResultResponse.builder()
                    .deviceDetail(DeviceDetailResponse.builder()
                            .deviceId(d.getDeviceId())
                            .deviceName(d.getDeviceName())
                            .serialNumber(d.getSerialNumber())
                            .modelName(m != null ? m.getModelName() : null)
                            .status(d.getStatus() != null ? d.getStatus().name() : null)
                            .warehouseName(
                                    d.getCurrentWarehouse() != null ? d.getCurrentWarehouse().getWarehouseName() : null)
                            .floorName(d.getCurrentFloor() != null ? d.getCurrentFloor().getFloorName() : null)
                            .assignedUserEid(d.getCurrentUser() != null ? d.getCurrentUser().getEid() : null)
                            .assignedUserName(d.getCurrentUser() != null ? d.getCurrentUser().getFullName() : null)
                            .note(null)
                            .build())
                    .build();
        } else if (devices.size() > 1) {
            List<SearchResultResponse.ResultItem> results = devices.stream()
                    .map(d -> SearchResultResponse.ResultItem.builder()
                            .type("DEVICE")
                            .deviceId(d.getDeviceId())
                            .deviceName(d.getDeviceName())
                            .serialNumber(d.getSerialNumber())
                            .modelName(d.getModel() != null ? d.getModel().getModelName() : null)
                            .status(d.getStatus() != null ? d.getStatus().name() : null)
                            .build())
                    .collect(Collectors.toList());
            return SearchResultResponse.builder().results(results).build();
        }
        // Không tìm thấy
        return SearchResultResponse.builder().results(Collections.emptyList()).build();
    }
}