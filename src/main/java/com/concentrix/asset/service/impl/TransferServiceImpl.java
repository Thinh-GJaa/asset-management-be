package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateTransferRequest;
import com.concentrix.asset.dto.response.TransferResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.TransferMapper;
import com.concentrix.asset.repository.TransactionRepository;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.repository.DeviceRepository;
import com.concentrix.asset.repository.DeviceWarehouseRepository;
import com.concentrix.asset.service.TransferService;
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
public class TransferServiceImpl implements TransferService {

    TransactionRepository transactionRepository;
    TransferMapper transferMapper;
    UserRepository userRepository;
    DeviceRepository deviceRepository;
    DeviceWarehouseRepository deviceWarehouseRepository;

    @Override
    public TransferResponse getTransferById(Integer transferId) {

        AssetTransaction transaction = transactionRepository.findById(transferId).orElseThrow(
                () -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, transferId));

        return transferMapper.toTransferResponse(transaction);
    }

    @Override
    public TransferResponse createTransfer(CreateTransferRequest request) {
        AssetTransaction transaction = transferMapper.toAssetTransaction(request);
        transaction.setCreatedBy(getCurrentUser());

        // Tạo danh sách TransactionDetail từ request.items
        AssetTransaction finalTransaction = transaction;
        List<TransactionDetail> details = request.getItems().stream()
                .map(item -> {
                    TransactionDetail detail = new TransactionDetail();
                    detail.setDevice(deviceRepository.findById(item.getDeviceId())
                            .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND, item.getDeviceId())));
                    detail.setQuantity(item.getQuantity());
                    detail.setTransaction(finalTransaction); // liên kết ngược
                    return detail;
                })
                .collect(Collectors.toList());

        transaction.setDetails(details);

        transaction = transactionRepository.save(transaction);
        updateWarehouses(transaction);

        return transferMapper.toTransferResponse(transaction);
    }

    @Override
    public Page<TransferResponse> filterTransfers(Pageable pageable) {
        return transactionRepository.findALLByTransactionType(TransactionType.TRANSFER_SITE, pageable)
                .map(transferMapper::toTransferResponse);
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
            Integer toWarehouseId = transaction.getToWarehouse().getWarehouseId();
            Integer qty = detail.getQuantity();
            Device device = detail.getDevice();

            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();

            if (hasSerial) {
                // Nếu thiết bị có serial, khi rời kho thì xóa khỏi warehouse
                DeviceWarehouse fromStock = deviceWarehouseRepository
                        .findByWarehouse_WarehouseIdAndDevice_DeviceId(fromWarehouseId, deviceId)
                        .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE, deviceId,
                                fromWarehouseId));
                deviceWarehouseRepository.delete(fromStock);

                // Thêm thiết bị vào kho đích (nếu chưa có)
                DeviceWarehouse toStock = deviceWarehouseRepository
                        .findByWarehouse_WarehouseIdAndDevice_DeviceId(toWarehouseId, deviceId)
                        .orElse(null);
                if (toStock == null) {
                    toStock = new DeviceWarehouse();
                    toStock.setDevice(device);
                    toStock.setWarehouse(transaction.getToWarehouse());
                    toStock.setQuantity(1);
                    deviceWarehouseRepository.save(toStock);
                }
            } else {

                // Xử lý theo số lượng
                DeviceWarehouse fromStock = deviceWarehouseRepository
                        .findByWarehouse_WarehouseIdAndDevice_DeviceId(fromWarehouseId, deviceId)
                        .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE, deviceId,
                                fromWarehouseId));
                if (fromStock.getQuantity() < qty) {
                    throw new CustomException(ErrorCode.STOCK_OUT, deviceId);
                }
                fromStock.setQuantity(fromStock.getQuantity() - qty);
                deviceWarehouseRepository.save(fromStock);

                DeviceWarehouse toStock = deviceWarehouseRepository
                        .findByWarehouse_WarehouseIdAndDevice_DeviceId(toWarehouseId, deviceId)
                        .orElse(null);
                if (toStock == null) {
                    toStock = new DeviceWarehouse();
                    toStock.setDevice(device);
                    toStock.setWarehouse(transaction.getToWarehouse());
                    toStock.setQuantity(0);
                }
                toStock.setQuantity(toStock.getQuantity() + qty);
                deviceWarehouseRepository.save(toStock);
            }
        }
    }
}
