package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.UpdateDeviceRequest;
import com.concentrix.asset.dto.response.DeviceResponse;
import com.concentrix.asset.entity.Device;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.DeviceMapper;
import com.concentrix.asset.repository.DeviceRepository;
import com.concentrix.asset.repository.ModelRepository;
import com.concentrix.asset.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class DeviceServiceImpl implements DeviceService {

    DeviceRepository deviceRepository;
    DeviceMapper deviceMapper;
    ModelRepository modelRepository;

    @Override
    public DeviceResponse getDeviceById(Integer deviceId) {
        return deviceRepository.findById(deviceId)
                .map(deviceMapper::toDeviceResponse)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND, deviceId));
    }

    @Override
    public DeviceResponse updateDevice(UpdateDeviceRequest request) {

        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND, request.getDeviceId()));

        modelRepository.findById(request.getModelId())
                .orElseThrow(() -> new CustomException(ErrorCode.MODEL_NOT_FOUND, request.getModelId()));

        if(request.getSerialNumber() != null && !request.getSerialNumber().isEmpty()) {
            if (deviceRepository.findBySerialNumber(request.getSerialNumber()).isPresent()
            && !device.getSerialNumber().equals(request.getSerialNumber())) {
                throw new CustomException(ErrorCode.SERIAL_NUMBER_ALREADY_EXISTS, request.getSerialNumber());
            }
        }

        deviceMapper.updateDevice(device, request);
        device = deviceRepository.save(device);
        log.info("[DeviceServiceImpl] Updated device with ID: {}", device.getDeviceId());
        return deviceMapper.toDeviceResponse(device);

    }

    @Override
    public Page<DeviceResponse> filterDevices(Pageable pageable, Integer modelId, com.concentrix.asset.enums.DeviceType type) {
        Page<Device> devices;
        if (modelId != null) {
            devices = deviceRepository.findByModel_ModelId(modelId, pageable);
        } else if (type != null) {
            devices = deviceRepository.findByModel_Type(type, pageable);
        } else {
            devices = deviceRepository.findAll(pageable);
        }
        return devices.map(deviceMapper::toDeviceResponse);
    }
}
