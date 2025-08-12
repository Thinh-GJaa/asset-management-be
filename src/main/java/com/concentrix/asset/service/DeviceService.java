package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.UpdateDeviceRequest;
import com.concentrix.asset.dto.response.DeviceResponse;
import com.concentrix.asset.dto.response.DeviceMovementHistoryResponse;
import com.concentrix.asset.dto.response.DeviceBorrowingInfoResponse;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DeviceService {
        DeviceResponse getDeviceById(Integer deviceId);

        DeviceResponse updateDevice(UpdateDeviceRequest request);

        Page<DeviceResponse> filterDevices(String search, DeviceType type, Integer modelId,DeviceStatus status, Pageable pageable);

        /**
         * Lấy lịch sử vòng đời thiết bị (mô tả dạng chuỗi từng transaction)
         */
        List<DeviceMovementHistoryResponse> getDeviceMovementHistoryBySerial(String serialNumber);

        /**
         * Lấy danh sách user và các thiết bị họ đang mượn (có quantity với thiết bị
         * không serial)
         */
        List<DeviceBorrowingInfoResponse> getAllUserBorrowingDevices();

        /**
         * Lấy danh sách thiết bị mà 1 user đang mượn (có quantity với thiết bị không
         * serial)
         */
        List<DeviceBorrowingInfoResponse.DeviceInfo> getBorrowingDevicesByUser(String eid);

        Page<DeviceBorrowingInfoResponse> getBorrowingDevice(Pageable pageable);

        List<String> getAllDeviceTypes();

        public List<String> getDeviceStatuses();

}