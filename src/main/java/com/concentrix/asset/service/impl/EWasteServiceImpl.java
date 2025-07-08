package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateEWasteRequest;
import com.concentrix.asset.dto.response.EWasteResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.EWasteMapper;
import com.concentrix.asset.repository.DeviceRepository;
import com.concentrix.asset.repository.DeviceWarehouseRepository;
import com.concentrix.asset.repository.TransactionRepository;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.service.EWasteService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class EWasteServiceImpl implements EWasteService {
    TransactionRepository transactionRepository;
    EWasteMapper ewasteMapper;
    UserRepository userRepository;
    DeviceRepository deviceRepository;
    DeviceWarehouseRepository deviceWarehouseRepository;

    @Override
    public EWasteResponse getEWasteById(Integer ewasteId) {
        AssetTransaction transaction = transactionRepository.findById(ewasteId).orElseThrow(
                () -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, ewasteId));
        return ewasteMapper.toEWasteResponse(transaction);
    }

    @Override
    public EWasteResponse createEWaste(CreateEWasteRequest request) {
        AssetTransaction transaction = ewasteMapper.toAssetTransaction(request);
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

        return ewasteMapper.toEWasteResponse(transaction);
    }

    @Override
    public Page<EWasteResponse> filterEWastes(Pageable pageable) {
        return transactionRepository.findALLByTransactionType(TransactionType.E_WASTE, pageable)
                .map(ewasteMapper::toEWasteResponse);
    }

    private User getCurrentUser() {
        String EID = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));
    }

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