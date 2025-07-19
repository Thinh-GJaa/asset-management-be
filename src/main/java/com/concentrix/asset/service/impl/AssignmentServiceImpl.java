package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateAssignmentRequest;
import com.concentrix.asset.dto.response.AssignmentResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.AssignmentMapper;
import com.concentrix.asset.repository.DeviceRepository;
import com.concentrix.asset.repository.DeviceWarehouseRepository;
import com.concentrix.asset.repository.TransactionRepository;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.service.AssignmentService;
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
public class AssignmentServiceImpl implements AssignmentService {

    TransactionRepository transactionRepository;
    AssignmentMapper assignmentMapper;
    UserRepository userRepository;
    DeviceRepository deviceRepository;
    DeviceWarehouseRepository deviceWarehouseRepository;

    @Override
    public AssignmentResponse getAssignmentById(Integer AssignmentId) {

        AssetTransaction transaction = transactionRepository.findById(AssignmentId).orElseThrow(
                () -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, AssignmentId));

        return assignmentMapper.toAssignmentResponse(transaction);
    }

    @Override
    public AssignmentResponse createAssignment(CreateAssignmentRequest request) {
        AssetTransaction transaction = assignmentMapper.toAssetTransaction(request);
        transaction.setCreatedBy(getCurrentUser());

        // Gán UserUse từ eid trong request
        User userUse = userRepository.findById(request.getEid())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, request.getEid()));
        transaction.setUserUse(userUse);

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
        updateWarehouses(transaction);

        return assignmentMapper.toAssignmentResponse(transaction);
    }

    @Override
    public Page<AssignmentResponse> filterAssignments(Pageable pageable) {
        return transactionRepository.findALLByTransactionType(TransactionType.ASSIGNMENT, pageable)
                .map(assignmentMapper::toAssignmentResponse);
    }

    private User getCurrentUser() {
        String EID = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));
    }

    private void updateWarehouses(AssetTransaction transaction) {
        for (TransactionDetail detail : transaction.getDetails()) {
            Device device = detail.getDevice();
            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
            if (hasSerial) {
                // Kiểm tra trạng thái device phải là IN_STOCK mới được cấp phát
                if (device.getStatus() != com.concentrix.asset.enums.DeviceStatus.IN_STOCK) {
                    throw new CustomException(ErrorCode.INVALID_DEVICE_STATUS, device.getSerialNumber());
                }
                // Bổ sung kiểm tra device có đúng ở warehouse không
                if (device.getCurrentWarehouse() == null || !device.getCurrentWarehouse().getWarehouseId()
                        .equals(transaction.getFromWarehouse().getWarehouseId())) {
                    throw new CustomException(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE, device.getSerialNumber(),
                            transaction.getFromWarehouse().getWarehouseName());
                }
                device.setStatus(com.concentrix.asset.enums.DeviceStatus.ASSIGNED);
                device.setCurrentUser(transaction.getUserUse());
                device.setCurrentWarehouse(null);
                device.setCurrentFloor(null);
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
                    throw new CustomException(ErrorCode.STOCK_OUT, deviceId);
                }
                fromStock.setQuantity(fromStock.getQuantity() - qty);
                deviceWarehouseRepository.save(fromStock);
            }
        }
    }
}
