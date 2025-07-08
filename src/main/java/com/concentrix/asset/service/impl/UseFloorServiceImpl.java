package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateUseFloorRequest;
import com.concentrix.asset.dto.response.UseFloorResponse;
import com.concentrix.asset.entity.*;
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

        // Hợp nhất các item trùng deviceId
        java.util.Map<Integer, Integer> deviceQtyMap = new java.util.HashMap<>();
        for (var item : request.getItems()) {
            deviceQtyMap.merge(item.getDeviceId(), item.getQuantity(), Integer::sum);
        }

        AssetTransaction finalTransaction = transaction;
        java.util.List<TransactionDetail> details = deviceQtyMap.entrySet().stream()
                .map(entry -> {
                    TransactionDetail detail = new TransactionDetail();
                    detail.setDevice(deviceRepository.findById(entry.getKey())
                            .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND, entry.getKey())));
                    detail.setQuantity(entry.getValue());
                    detail.setTransaction(finalTransaction);
                    return detail;
                })
                .collect(java.util.stream.Collectors.toList());

        transaction.setDetails(details);

        transaction = transactionRepository.save(transaction);
        updateWarehouses(transaction);

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
    private void updateWarehouses(AssetTransaction transaction) {
        for (TransactionDetail detail : transaction.getDetails()) {
            Integer deviceId = detail.getDevice().getDeviceId();
            Integer fromWarehouseId = transaction.getFromWarehouse().getWarehouseId();
            Integer qty = detail.getQuantity();
            Device device = detail.getDevice();
            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
            DeviceWarehouse fromStock = deviceWarehouseRepository
                    .findByWarehouse_WarehouseIdAndDevice_DeviceId(fromWarehouseId, deviceId)
                    .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE, deviceId,
                            fromWarehouseId));
            if (hasSerial) {
                deviceWarehouseRepository.delete(fromStock);
            } else {
                if (fromStock.getQuantity() < qty) {
                    throw new CustomException(ErrorCode.STOCK_OUT, deviceId);
                }
                fromStock.setQuantity(fromStock.getQuantity() - qty);
                deviceWarehouseRepository.save(fromStock);
            }
        }
    }
}