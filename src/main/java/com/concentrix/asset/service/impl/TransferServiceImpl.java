package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateTransferRequest;
import com.concentrix.asset.dto.response.TransferResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.enums.TransactionStatus;
import com.concentrix.asset.enums.DeviceStatus;
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
        transaction.setTransactionStatus(TransactionStatus.PENDING);

        // Tạo danh sách TransactionDetail từ request.items
        AssetTransaction finalTransaction = transaction;
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

                    TransactionDetail detail = new TransactionDetail();
                    detail.setDevice(device);
                    detail.setQuantity(item.getQuantity());
                    detail.setTransaction(finalTransaction); // liên kết ngược
                    return detail;
                })
                .collect(Collectors.toList());

        transaction.setDetails(details);

        transaction = transactionRepository.save(transaction);
        updateDeviceAndWarehousesForTransfer(transaction);

        return transferMapper.toTransferResponse(transaction);
    }

    public void confirmTransfer(Integer transactionId) {
        AssetTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, transactionId));

        if (transaction.getTransactionType() != TransactionType.TRANSFER_SITE) {
            throw new CustomException(ErrorCode.TRANSACTION_TYPE_INVALID, transactionId);
        }

        if (transaction.getTransactionStatus() != TransactionStatus.PENDING) {
            throw new CustomException(ErrorCode.TRANSACTION_STATUS_INVALID, transactionId);
        }

        transaction.setTransactionStatus(TransactionStatus.CONFIRMED);

        for (TransactionDetail detail : transaction.getDetails()) {
            Device device = detail.getDevice();
            if (device.getSerialNumber() != null && !device.getSerialNumber().isEmpty()) {
                device.setStatus(DeviceStatus.IN_STOCK);
                device.setCurrentWarehouse(transaction.getToWarehouse());
                deviceRepository.save(device);
            } else {
                // Cộng vào toWarehouse khi xác nhận đối với non-serial device
                Integer deviceId = device.getDeviceId();
                Integer toWarehouseId = transaction.getToWarehouse().getWarehouseId();
                Integer qty = detail.getQuantity();
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
        transactionRepository.save(transaction);
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

    private void updateDeviceAndWarehousesForTransfer(AssetTransaction transaction) {
        for (TransactionDetail detail : transaction.getDetails()) {
            Device device = detail.getDevice();
            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
            if (hasSerial) {
                // Bổ sung kiểm tra device có đúng ở warehouse không
                if (device.getCurrentWarehouse() == null || !device.getCurrentWarehouse().getWarehouseId()
                        .equals(transaction.getFromWarehouse().getWarehouseId())) {
                    throw new CustomException(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE, device.getSerialNumber(),
                            transaction.getFromWarehouse().getWarehouseName());
                }
                device.setStatus(DeviceStatus.ON_THE_MOVE);
                device.setCurrentWarehouse(null);
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
                // Không cộng vào toWarehouse ở bước tạo transfer
            }
        }
    }
}
