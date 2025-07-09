package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateReturnFromFloorRequest;
import com.concentrix.asset.dto.response.ReturnFromFloorResponse;
import com.concentrix.asset.entity.*;
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

        // Hợp nhất các item trùng deviceId
        java.util.Map<Integer, Integer> deviceQtyMap = new java.util.HashMap<>();
        for (var item : request.getItems()) {
            deviceQtyMap.merge(item.getDeviceId(), item.getQuantity(), Integer::sum);
        }

        AssetTransaction finalTransaction = transaction;
        java.util.List<TransactionDetail> details = deviceQtyMap.entrySet().stream()
                .map(entry -> {
                    Device device = deviceRepository.findById(entry.getKey())
                            .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND, entry.getKey()));
                    // Nếu có serial, kiểm tra transaction cuối cùng phải nằm trong fromFloor
                    if (device.getSerialNumber() != null && !device.getSerialNumber().isEmpty()) {
                        TransactionDetail lastDetail = transactionDetailRepository
                                .findFirstByDevice_DeviceIdOrderByTransaction_TransactionIdDesc(device.getDeviceId());
                        if (lastDetail == null || lastDetail.getTransaction() == null) {
                            throw new CustomException(ErrorCode.INVALID_DEVICE_STATUS, device.getDeviceId());
                        }
                        AssetTransaction lastTrans = lastDetail.getTransaction();
                        Integer lastFloorId = null;
                        if (lastTrans.getToFloor() != null) {
                            lastFloorId = lastTrans.getToFloor().getFloorId();
                        } else if (lastTrans.getFromFloor() != null) {
                            lastFloorId = lastTrans.getFromFloor().getFloorId();
                        }
                        if (lastFloorId == null || !lastFloorId.equals(request.getFromFloorId())) {
                            throw new CustomException(ErrorCode.INVALID_DEVICE_STATUS, device.getDeviceId());
                        }
                    }
                    TransactionDetail detail = new TransactionDetail();
                    detail.setDevice(device);
                    detail.setQuantity(entry.getValue());
                    detail.setTransaction(finalTransaction);
                    return detail;
                })
                .collect(java.util.stream.Collectors.toList());

        transaction.setDetails(details);

        transaction = transactionRepository.save(transaction);
        updateWarehouses(transaction);

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

    // Cộng kho vào toWarehouse (cả serial và không serial)
    private void updateWarehouses(AssetTransaction transaction) {
        for (TransactionDetail detail : transaction.getDetails()) {
            Integer deviceId = detail.getDevice().getDeviceId();
            Integer toWarehouseId = transaction.getToWarehouse().getWarehouseId();
            Integer qty = detail.getQuantity();
            Device device = detail.getDevice();
            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
            DeviceWarehouse toStock = deviceWarehouseRepository
                    .findByWarehouse_WarehouseIdAndDevice_DeviceId(toWarehouseId, deviceId)
                    .orElse(null);
            if (hasSerial) {
                if (toStock == null) {
                    toStock = new DeviceWarehouse();
                    toStock.setDevice(device);
                    toStock.setWarehouse(transaction.getToWarehouse());
                    toStock.setQuantity(1);
                } else {
                    toStock.setQuantity(toStock.getQuantity() + 1);
                }
                deviceWarehouseRepository.save(toStock);
            } else {
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