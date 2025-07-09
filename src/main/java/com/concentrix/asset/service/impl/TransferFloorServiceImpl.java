package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateTransferFloorRequest;
import com.concentrix.asset.dto.response.TransferFloorResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.TransferFloorMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.TransferFloorService;
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
public class TransferFloorServiceImpl implements TransferFloorService {
    TransactionRepository transactionRepository;
    TransferFloorMapper transferFloorMapper;
    DeviceRepository deviceRepository;
    DeviceWarehouseRepository deviceWarehouseRepository;
    FloorRepository floorRepository;
    UserRepository userRepository;
    TransactionDetailRepository transactionDetailRepository;

    @Override
    public TransferFloorResponse getTransferFloorById(Integer transferFloorId) {
        AssetTransaction transaction = transactionRepository.findById(transferFloorId).orElseThrow(
                () -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, transferFloorId));
        return transferFloorMapper.toTransferFloorResponse(transaction);
    }

    @Override
    public TransferFloorResponse createTransferFloor(CreateTransferFloorRequest request) {
        AssetTransaction transaction = transferFloorMapper.toAssetTransaction(request);
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
                    // Nếu có serial, kiểm tra device phải nằm trong fromFloor dựa trên transaction
                    // cuối cùng
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

        return transferFloorMapper.toTransferFloorResponse(transaction);
    }

    @Override
    public Page<TransferFloorResponse> filterTransferFloors(Pageable pageable) {
        return transactionRepository.findALLByTransactionType(TransactionType.TRANSFER_FLOOR, pageable)
                .map(transferFloorMapper::toTransferFloorResponse);
    }

    private User getCurrentUser() {
        String EID = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));
    }

    // Trừ kho khỏi fromFloor (cả serial và không serial)
    private void updateWarehouses(AssetTransaction transaction) {
        for (TransactionDetail detail : transaction.getDetails()) {
            Integer deviceId = detail.getDevice().getDeviceId();
            Integer fromFloorId = transaction.getFromFloor().getFloorId();
            Integer qty = detail.getQuantity();
            Device device = detail.getDevice();
            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
            // Giả sử có DeviceFloorRepository để quản lý tồn kho trên từng sàn
            // DeviceFloor fromStock =
            // deviceFloorRepository.findByFloor_FloorIdAndDevice_DeviceId(fromFloorId,
            // deviceId).orElseThrow(...);
            // if (hasSerial) { ... } else { ... }
            // (Tùy vào thiết kế thực tế, bạn cần bổ sung DeviceFloor entity/repository nếu
            // muốn quản lý tồn kho trên sàn)
        }
    }
}