package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateReturnFromUserRequest;
import com.concentrix.asset.dto.response.ReturnFromUserResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.ReturnFromUserMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.ReturnFromUserService;
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
public class ReturnFromUserServiceImpl implements ReturnFromUserService {
    TransactionRepository transactionRepository;
    ReturnFromUserMapper returnFromUserMapper;
    UserRepository userRepository;
    DeviceRepository deviceRepository;
    DeviceWarehouseRepository deviceWarehouseRepository;
    TransactionDetailRepository transactionDetailRepository;

    @Override
    public ReturnFromUserResponse getReturnFromUserById(Integer returnId) {
        AssetTransaction transaction = transactionRepository.findById(returnId).orElseThrow(
                () -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, returnId));
        return returnFromUserMapper.toReturnFromUserResponse(transaction);
    }

    @Override
    public ReturnFromUserResponse createReturnFromUser(CreateReturnFromUserRequest request) {
        AssetTransaction transaction = returnFromUserMapper.toAssetTransaction(request);
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
                    // Nếu có serial, kiểm tra transaction cuối cùng phải là ASSIGNMENT cho user đó
                    if (device.getSerialNumber() != null && !device.getSerialNumber().isEmpty()) {
                        TransactionDetail lastDetail = transactionDetailRepository
                                .findFirstByDevice_DeviceIdOrderByTransaction_TransactionIdDesc(device.getDeviceId());
                        if (lastDetail == null || lastDetail.getTransaction() == null
                                || lastDetail.getTransaction().getTransactionType() != TransactionType.ASSIGNMENT
                                || lastDetail.getTransaction().getUserUse() == null
                                || !lastDetail.getTransaction().getUserUse().getEid().equals(request.getEid())) {
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

        // Gán userUse cho transaction
        User userUse = userRepository.findById(request.getEid())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, request.getEid()));
        transaction.setUserUse(userUse);

        transaction = transactionRepository.save(transaction);
        updateWarehouses(transaction);

        return returnFromUserMapper.toReturnFromUserResponse(transaction);
    }

    @Override
    public Page<ReturnFromUserResponse> filterReturnFromUsers(Pageable pageable) {
        return transactionRepository.findALLByTransactionType(TransactionType.RETURN_FROM_USER, pageable)
                .map(returnFromUserMapper::toReturnFromUserResponse);
    }

    private User getCurrentUser() {
        String EID = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));
    }

    // Cộng kho khi trả về (cả serial và không serial)
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