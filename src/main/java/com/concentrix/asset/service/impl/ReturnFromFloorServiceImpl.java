package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateReturnFromFloorRequest;
import com.concentrix.asset.dto.response.ReturnFromFloorResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.ReturnFromFloorMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.ReturnFromFloorService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ReturnFromFloorServiceImpl implements ReturnFromFloorService {
    TransactionRepository transactionRepository;
    ReturnFromFloorMapper returnFromFloorMapper;
    DeviceRepository deviceRepository;
    DeviceWarehouseRepository deviceWarehouseRepository;
    FloorRepository floorRepository;
    UserRepository userRepository;
    TransactionDetailRepository transactionDetailRepository;

    @Override
    public ReturnFromFloorResponse getReturnFromFloorById(Integer returnFromFloorId) {
        AssetTransaction transaction = transactionRepository.findById(returnFromFloorId).orElseThrow(
                () -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, returnFromFloorId));
        return returnFromFloorMapper.toReturnFromFloorResponse(transaction);
    }

    @Override
    public ReturnFromFloorResponse createReturnFromFloor(CreateReturnFromFloorRequest request) {
        AssetTransaction transaction = returnFromFloorMapper.toAssetTransaction(request);
        transaction.setCreatedBy(getCurrentUser());

        // Kiểm tra trùng lặp deviceId trong danh sách
        Set<Integer> duplicateCheckSet = new HashSet<>();
        for (var item : request.getItems()) {
            if (!duplicateCheckSet.add(item.getDeviceId())) {
                throw new CustomException(ErrorCode.DUPLICATE_SERIAL_NUMBER, item.getDeviceId());
            }
        }

        final AssetTransaction finalTransaction = transaction;
        List<TransactionDetail> details = request.getItems().stream()
                .map(item -> {
                    Device device = deviceRepository.findById(item.getDeviceId())
                            .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND, item.getDeviceId()));
                    boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
                    if (hasSerial) {
                        // Serial: chỉ cho phép trả về kho nếu đang IN_FLOOR
                        if (device.getStatus() != DeviceStatus.IN_FLOOR) {
                            throw new CustomException(ErrorCode.INVALID_DEVICE_STATUS, device.getSerialNumber());
                        }
                    }
                    TransactionDetail detail = new TransactionDetail();
                    detail.setDevice(device);
                    detail.setQuantity(item.getQuantity());
                    detail.setTransaction(finalTransaction);
                    return detail;
                })
                .collect(Collectors.toList());

        transaction.setDetails(details);

        transaction = transactionRepository.save(transaction);
        updateStockForReturnFromFloor(transaction);

        return returnFromFloorMapper.toReturnFromFloorResponse(transaction);
    }

    @Override
    public Page<ReturnFromFloorResponse> filterReturnFromFloors(Pageable pageable) {
        return transactionRepository.findALLByTransactionType(TransactionType.RETURN_FROM_FLOOR, pageable)
                .map(returnFromFloorMapper::toReturnFromFloorResponse);
    }

    private User getCurrentUser() {
        String EID = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));
    }

    private void updateStockForReturnFromFloor(AssetTransaction transaction) {
        for (TransactionDetail detail : transaction.getDetails()) {
            Device device = detail.getDevice();
            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
            if (hasSerial) {
                // Serial: chỉ update Device, không động vào DeviceWarehouse
                device.setStatus(DeviceStatus.IN_STOCK);
                device.setCurrentWarehouse(transaction.getToWarehouse());
                device.setCurrentFloor(null);
                deviceRepository.save(device);
            } else {
                Integer deviceId = device.getDeviceId();
                Integer toWarehouseId = transaction.getToWarehouse().getWarehouseId();
                Integer qty = detail.getQuantity();
                DeviceWarehouse fromStock = deviceWarehouseRepository
                        .findByWarehouse_WarehouseIdAndDevice_DeviceId(toWarehouseId, deviceId)
                        .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE,
                                device.getModel().getModelName(),
                                transaction.getFromFloor().getFloorName()));
                if (fromStock.getQuantity() < qty) {
                    throw new CustomException(ErrorCode.STOCK_OUT, device.getModel().getModelName());
                }
                fromStock.setQuantity(fromStock.getQuantity() - qty);
                deviceWarehouseRepository.save(fromStock);
                // Cộng về kho
                DeviceWarehouse toStock = deviceWarehouseRepository
                        .findByWarehouse_WarehouseIdAndDevice_DeviceId(toWarehouseId, deviceId)
                        .orElse(null);
                if (toStock == null) {
                    toStock = new DeviceWarehouse();
                    toStock.setDevice(device);
                    toStock.setWarehouse(transaction.getToWarehouse());
                    toStock.setQuantity(qty);
                } else {
                    toStock.setQuantity(toStock.getQuantity() + qty);
                }
                deviceWarehouseRepository.save(toStock);
            }
        }
    }
}