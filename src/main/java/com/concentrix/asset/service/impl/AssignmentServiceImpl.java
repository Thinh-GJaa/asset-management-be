package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateAssignmentRequest;
import com.concentrix.asset.dto.response.AssetHandoverResponse;
import com.concentrix.asset.dto.response.AssignmentResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

        // Gom các serialNumber không tìm thấy vào một list
        List<String> serialNotFound = new ArrayList<>();
        AssetTransaction finalTransaction = transaction;
        List<TransactionDetail> details = request.getItems().stream()
                .map(item -> {
                    final Device device;
                    if (item.getSerialNumber() != null && !item.getSerialNumber().isEmpty()) {
                        // Tìm device theo serial number - gom lỗi vào list
                        device = deviceRepository.findBySerialNumber(item.getSerialNumber())
                                .orElse(null);
                        if (device == null) {
                            serialNotFound.add(item.getSerialNumber());
                            return null; // sẽ filter sau
                        }
                    } else if (item.getModelId() != null) {
                        // Tìm device theo modelId - báo lỗi từng cái
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
                .filter(detail -> detail != null)
                .collect(Collectors.toList());

        // Nếu có serialNumber không tìm thấy thì trả về list
        if (!serialNotFound.isEmpty()) {
            throw new CustomException(ErrorCode.DEVICE_NOT_FOUND, String.join(",", serialNotFound));
        }

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

    @Override
    public AssetHandoverResponse getAssetHandoverByAssignmentId(Integer assignmentId) {
        AssetTransaction assignment = transactionRepository.findById(assignmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        // Kiểm tra xem có phải là ASSIGNMENT transaction không
        if (assignment.getTransactionType() != TransactionType.ASSIGNMENT) {
            throw new CustomException(ErrorCode.ASSIGNMENT_NOT_FOUND);
        }

        // Lấy thông tin người dùng
        String endUser = assignment.getUserUse() != null ? assignment.getUserUse().getFullName() : "N/A";
        String employeeId = assignment.getUserUse() != null ? assignment.getUserUse().getEid() : "N/A";
        String ssoEmail = assignment.getUserUse() != null ? assignment.getUserUse().getEmail() : "N/A";
        String msa = assignment.getUserUse() != null ? assignment.getUserUse().getMsa() : "N/A";
        String role = assignment.getUserUse() != null ? assignment.getUserUse().getJobTitle() : "N/A";
        String location = assignment.getUserUse() != null ? assignment.getFromWarehouse().getSite().getSiteName() : "N/A";

        // Lấy thông tin IT person (người tạo)
        String itPerson = assignment.getCreatedBy() != null ? assignment.getCreatedBy().getFullName() + " - IT" : "N/A";

        // Format ngày
        String issueDate = assignment.getCreatedAt() != null ?
                assignment.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MMM/yyyy")) : "N/A";

        // Lấy danh sách thiết bị
        List<AssetHandoverResponse.AssetHandoverDetailResponse> assets = assignment.getDetails().stream()
                .map(this::mapTransactionDetailToAssetDetail)
                .collect(Collectors.toList());

        return AssetHandoverResponse.builder()
                .transactionId(assignment.getTransactionId())
                .itPerson(itPerson)
                .location(location)
                .endUser(endUser)
                .msa(msa)
                .employeeId(employeeId)
                .ssoEmail(ssoEmail)
                .assetType("Permanent") // Mặc định là Permanent cho Assignment
                .issueDate(issueDate)
                .role(role)
                .createdAt(assignment.getCreatedAt())
                .createdBy(assignment.getCreatedBy() != null ? assignment.getCreatedBy().getFullName() : "N/A")
                .assets(assets)
                .build();
    }

    private AssetHandoverResponse.AssetHandoverDetailResponse mapTransactionDetailToAssetDetail(TransactionDetail detail) {
        return AssetHandoverResponse.AssetHandoverDetailResponse.builder()
                .deviceId(detail.getDevice().getDeviceId())
                .name(detail.getDevice().getDeviceName())
                .serialNumber(detail.getDevice().getSerialNumber())
                .quantity(detail.getQuantity())
                .remark("good") // Mặc định là good
                .build();
    }



    private User getCurrentUser() {
        String EID = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));
    }

    private void updateWarehouses(AssetTransaction transaction) {
        List<String> serialInvalid = new ArrayList<>();
        for (TransactionDetail detail : transaction.getDetails()) {
            Device device = detail.getDevice();
            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
            if (hasSerial) {
                // Kiểm tra trạng thái device phải là IN_STOCK mới được cấp phát
                if (device.getStatus() != DeviceStatus.IN_STOCK) {
                    serialInvalid.add(device.getSerialNumber());
                }
                // Bổ sung kiểm tra device có đúng ở warehouse không
                if (device.getCurrentWarehouse() == null || !device.getCurrentWarehouse().getWarehouseId()
                        .equals(transaction.getFromWarehouse().getWarehouseId())) {
                    serialInvalid.add(device.getSerialNumber());
                }
                device.setStatus(DeviceStatus.ASSIGNED);
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
        if (!serialInvalid.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_DEVICE_STATUS, String.join(",", serialInvalid));
        }
    }
}
