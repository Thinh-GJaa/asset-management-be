package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.UpdateDeviceRequest;
import com.concentrix.asset.dto.response.DeviceResponse;
import com.concentrix.asset.enums.DeviceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeviceService {
    DeviceResponse getDeviceById(Integer deviceId);

    DeviceResponse updateDevice(UpdateDeviceRequest request);

    Page<DeviceResponse> filterDevices(Pageable pageable, Integer modelId, DeviceType type);
}