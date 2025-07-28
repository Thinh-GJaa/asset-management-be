package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateUseFloorRequest;
import com.concentrix.asset.dto.response.UseFloorResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.UseFloorMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.UseFloorService;
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
public class UseFloorServiceImpl implements UseFloorService {
    TransactionRepository transactionRepository;
    UseFloorMapper useFloorMapper;
    DeviceRepository deviceRepository;
    DeviceWarehouseRepository deviceWarehouseRepository;
    FloorRepository floorRepository;
    UserRepository userRepository;

    @Override
    public UseFloorResponse getUseFloorById(Integer useFloorId) {
        AssetTransaction transaction = transactionRepository.findById(useFloorId).orElseThrow(
                () -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, useFloorId));
        return useFloorMapper.toUseFloorResponse(transaction);
    }

    @Override
    public UseFloorResponse createUseFloor(CreateUseFloorRequest request) {
        AssetTransaction transaction = useFloorMapper.toAssetTransaction(request);
        transaction.setCreatedBy(getCurrentUser());

        // Kiểm tra trùng lặp serialNumber hoặc modelId trong danh sách
        Set<String> duplicateSerialCheckSet = new HashSet<>();
        Set<Integer> duplicateModelCheckSet = new HashSet<>();
        for (var item : request.getItems()) {
            if (item.getSerialNumber() != null && !item.getSerialNumber().isEmpty()) {
                if (!duplicateSerialCheckSet.add(item.getSerialNumber())) {
                    throw new CustomException(ErrorCode.DUPLICATE_SERIAL_NUMBER, item.getSerialNumber());
                }
            } else if (item.getModelId() != null) {
                if (!duplicateModelCheckSet.add(item.getModelId())) {
                    throw new CustomException(ErrorCode.DUPLICATE_SERIAL_NUMBER, "Model ID: " + item.getModelId());
                }
            }
        }

        final AssetTransaction finalTransaction = transaction;
        List<TransactionDetail> details = request.getItems().stream()
                .map(item -> {
                    // Tìm device dựa trên serialNumber hoặc modelId
                    final Device device;
                    if (item.getSerialNumber() != null && !item.getSerialNumber().isEmpty()) {
                        // Tìm device theo serial number - đây là thiết bị cụ thể
                        device = deviceRepository.findBySerialNumber(item.getSerialNumber())
                                .orElseThrow(
                                        () -> new CustomException(ErrorCode.DEVICE_NOT_FOUND, item.getSerialNumber()));
                    } else if (item.getModelId() != null) {
                        // Tìm device theo modelId - đây là thiết bị không có serial
                        // Lấy device đầu tiên của model đó
                        device = deviceRepository.findFirstByModel_ModelId(item.getModelId())
                                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND,
                                        "Model ID: " + item.getModelId()));
                    } else {
                        throw new CustomException(ErrorCode.DEVICE_NOT_FOUND,
                                "Either serialNumber or modelId must be provided");
                    }

                    boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();

                    if (hasSerial) {
                        // Serial: chỉ cho phép chuyển lên sàn nếu đang IN_STOCK
                        if (device.getStatus() != DeviceStatus.IN_STOCK) {
                            throw new CustomException(ErrorCode.INVALID_DEVICE_STATUS, device.getSerialNumber());
                        }
                        // Bổ sung kiểm tra device có đúng ở warehouse không
                        if (device.getCurrentWarehouse() == null || !device.getCurrentWarehouse().getWarehouseId()
                                .equals(finalTransaction.getFromWarehouse().getWarehouseId())) {
                            throw new CustomException(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE, device.getSerialNumber(),
                                    finalTransaction.getFromWarehouse().getWarehouseName());
                        }

                    } else {
                        // Non-serial: kiểm tra tồn kho trước khi chuyển lên sàn
                        Integer fromWarehouseId = finalTransaction.getFromWarehouse().getWarehouseId();
                        Integer qty = item.getQuantity();

                        DeviceWarehouse fromStock = deviceWarehouseRepository
                                .findByWarehouse_WarehouseIdAndDevice_DeviceId(fromWarehouseId, device.getDeviceId())
                                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE,
                                        device.getModel().getModelName(),
                                        finalTransaction.getFromWarehouse().getWarehouseName()));

                        if (fromStock.getQuantity() < qty) {
                            throw new CustomException(ErrorCode.STOCK_OUT, device.getModel().getModelName());
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
        updateStockForUseFloor(transaction);

        return useFloorMapper.toUseFloorResponse(transaction);
    }

    @Override
    public Page<UseFloorResponse> filterUseFloors(Pageable pageable) {
        return transactionRepository.findALLByTransactionType(TransactionType.USE_FLOOR, pageable)
                .map(useFloorMapper::toUseFloorResponse);
    }

    private User getCurrentUser() {
        String EID = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));
    }

    // Trừ kho khi chuyển lên sàn (cả serial và không serial)
    private void updateStockForUseFloor(AssetTransaction transaction) {
        for (TransactionDetail detail : transaction.getDetails()) {
            Device device = detail.getDevice();

            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();

            if (hasSerial) {
                // Serial: chỉ update Device, không động vào DeviceWarehouse
                device.setStatus(DeviceStatus.IN_FLOOR);
                device.setCurrentFloor(transaction.getToFloor());
                device.setCurrentWarehouse(null);
                device.setCurrentUser(null);
                deviceRepository.save(device);
            } else {
                Integer deviceId = device.getDeviceId();
                Integer fromWarehouseId = transaction.getFromWarehouse().getWarehouseId();
                Integer qty = detail.getQuantity();
                DeviceWarehouse fromStock = deviceWarehouseRepository
                        .findByWarehouse_WarehouseIdAndDevice_DeviceId(fromWarehouseId, deviceId)
                        .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE,
                                device.getModel().getModelName(),
                                transaction.getFromWarehouse().getWarehouseName()));
                if (fromStock.getQuantity() < qty) {
                    throw new CustomException(ErrorCode.STOCK_OUT, device.getModel().getModelName());
                }
                fromStock.setQuantity(fromStock.getQuantity() - qty);
                deviceWarehouseRepository.save(fromStock);
            }
        }
    }
}