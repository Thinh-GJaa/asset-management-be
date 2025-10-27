package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.UpdateDeviceRequest;
import com.concentrix.asset.dto.request.UpdateSeatNumberRequest;
import com.concentrix.asset.dto.response.DeviceBorrowingInfoResponse;
import com.concentrix.asset.dto.response.DeviceMovementHistoryResponse;
import com.concentrix.asset.dto.response.DeviceResponse;
import com.concentrix.asset.entity.Device;
import com.concentrix.asset.entity.Floor;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DeviceService {
    DeviceResponse getDeviceById(Integer deviceId);

    DeviceResponse updateDevice(UpdateDeviceRequest request);

    Page<DeviceResponse> filterDevices(String search, DeviceType type, Integer modelId, DeviceStatus status,
            Pageable pageable);

    List<DeviceMovementHistoryResponse> getDeviceMovementHistoryBySerial(String serialNumber);

    List<DeviceBorrowingInfoResponse.DeviceInfo> getBorrowingDevicesByUser(String eid);

    Page<DeviceBorrowingInfoResponse> getUsersBorrowingDevice(Pageable pageable);

    List<String> getAllDeviceTypes();

    List<String> getDeviceStatuses();

    String generateHostNameForDesktop(Device device, Floor floor);

    String generateHostNameForLaptop(Device device);

    void updateSeatNumber(List<UpdateSeatNumberRequest> request);

    Page<DeviceResponse> filterDevicesNonSeatNumber(String search, Integer siteId, Integer floorId,
            Pageable pageable);

    void deleteDevice(Integer deviceId);
}