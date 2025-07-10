package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateTransferFloorRequest;
import com.concentrix.asset.dto.response.TransferFloorResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
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

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

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
                        // Serial: chỉ cho phép chuyển sàn nếu đang IN_FLOOR, chưa có kiểm tra là đúng
                        // floor hay không
                        if (device.getStatus() != DeviceStatus.IN_FLOOR) {
                            throw new CustomException(ErrorCode.INVALID_DEVICE_STATUS, device.getSerialNumber());
                        }
                    }
                    // Non-serial: không kiểm tra tồn kho theo floor, chỉ update trạng thái nếu cần
                    TransactionDetail detail = new TransactionDetail();
                    detail.setDevice(device);
                    detail.setQuantity(item.getQuantity());
                    detail.setTransaction(finalTransaction);
                    return detail;
                })
                .collect(Collectors.toList());

        transaction.setDetails(details);

        transaction = transactionRepository.save(transaction);
        updateStockForTransferFloor(transaction);

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

    // Cập nhật trạng thái thiết bị sau khi chuyển sàn
    private void updateStockForTransferFloor(AssetTransaction transaction) {
        for (TransactionDetail detail : transaction.getDetails()) {
            Device device = detail.getDevice();
            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
            if (hasSerial) {
                device.setCurrentFloor(transaction.getToFloor());
                deviceRepository.save(device);
            }
        }
    }
}