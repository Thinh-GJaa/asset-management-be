package com.concentrix.asset.service.impl.transaction;

import com.concentrix.asset.dto.request.CreateReturnFromUserRequest;
import com.concentrix.asset.dto.response.AssetHandoverResponse;
import com.concentrix.asset.dto.response.ReturnFromUserResponse;
import com.concentrix.asset.entity.*;

import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.AssignmentMapper;
import com.concentrix.asset.mapper.ReturnFromUserMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.transaction.ReturnFromUserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

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
    AssignmentMapper assignmentMapper;

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

        // Kiểm tra trùng lặp serialNumber hoặc modelId trong danh sách trả về
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

        // Gom các serial not found/invalid vào list
        java.util.List<String> serialNotFound = new java.util.ArrayList<>();
        java.util.List<String> serialInvalid = new java.util.ArrayList<>();
        AssetTransaction finalTransaction = transaction;
        List<TransactionDetail> details = request.getItems().stream()
                .map(item -> {
                    final Device device;
                    if (item.getSerialNumber() != null && !item.getSerialNumber().isEmpty()) {
                        device = deviceRepository.findBySerialNumber(item.getSerialNumber())
                                .orElse(null);
                        if (device == null) {
                            serialNotFound.add(item.getSerialNumber());
                            return null;
                        }
                    } else if (item.getModelId() != null) {
                        device = deviceRepository.findFirstByModel_ModelId(item.getModelId())
                                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND,
                                        "Model ID: " + item.getModelId()));
                    } else {
                        throw new CustomException(ErrorCode.DEVICE_NOT_FOUND,
                                "Either serialNumber or modelId must be provided");
                    }

                    boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
                    if (hasSerial) {
                        // Serial: chỉ cho phép trả nếu đang ASSIGNED cho đúng user
                        if (device.getStatus() != DeviceStatus.ASSIGNED) {
                            serialInvalid.add(device.getSerialNumber());
                            return null;
                        }
                        if (device.getCurrentUser() == null
                                || !device.getCurrentUser().getEid().equals(request.getEid())) {
                            serialInvalid.add(device.getSerialNumber());
                            return null;
                        }
                    } else {
                        // Non-serial: kiểm tra tổng số lượng user đang mượn
                        List<TransactionDetail> allDetails = transactionDetailRepository
                                .findAllByDevice_DeviceIdAndTransaction_UserUse_Eid(device.getDeviceId(),
                                        request.getEid());
                        int totalAssigned = allDetails.stream()
                                .filter(d -> d.getTransaction()
                                        .getTransactionType() == TransactionType.ASSIGNMENT)
                                .mapToInt(TransactionDetail::getQuantity).sum();
                        int totalReturned = allDetails.stream()
                                .filter(d -> d.getTransaction()
                                        .getTransactionType() == TransactionType.RETURN_FROM_USER)
                                .mapToInt(TransactionDetail::getQuantity).sum();
                        int currentlyBorrowed = totalAssigned - totalReturned;
                        if (item.getQuantity() > currentlyBorrowed) {
                            throw new CustomException(ErrorCode.RETURN_QUANTITY_EXCEEDS_BORROWED,
                                    device.getModel().getModelName());
                        }
                    }
                    TransactionDetail detail = new TransactionDetail();
                    detail.setDevice(device);
                    detail.setQuantity(item.getQuantity());
                    detail.setTransaction(finalTransaction);
                    return detail;
                })
                .filter(detail -> detail != null)
                .collect(Collectors.toList());

        if (!serialNotFound.isEmpty()) {
            throw new CustomException(ErrorCode.DEVICE_NOT_FOUND, String.join(",", serialNotFound));
        }
        if (!serialInvalid.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_DEVICE_STATUS, String.join(",", serialInvalid));
        }

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
    public Page<ReturnFromUserResponse> filterReturnFromUsers(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
        return transactionRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("transactionType"), TransactionType.RETURN_FROM_USER));
            if (search != null && !search.isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("userUse").get("fullName")), searchPattern),
                        cb.like(cb.lower(root.get("userUse").get("eid")), searchPattern),
                        cb.like(cb.lower(root.get("toWarehouse").get("warehouseName")), searchPattern),
                        cb.like(cb.lower(root.get("createdBy").get("fullName")), searchPattern)

                ));
            }
            if (fromDate != null) {
                LocalDateTime fromDateTime = fromDate.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDateTime));
            }
            if (toDate != null) {
                LocalDateTime toDateTime = toDate.atTime(23, 59, 59);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDateTime));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable).map(returnFromUserMapper::toReturnFromUserResponse);
    }

    private User getCurrentUser() {
        String EID = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));
    }

    // Cộng kho khi trả về (cả serial và không serial)
    private void updateWarehouses(AssetTransaction transaction) {
        for (TransactionDetail detail : transaction.getDetails()) {
            Device device = detail.getDevice();
            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
            if (hasSerial) {
                // Serial: update Device về IN_STOCK, clear user, set warehouse
                device.setStatus(DeviceStatus.IN_STOCK);
                device.setCurrentUser(null);
                device.setCurrentWarehouse(transaction.getToWarehouse());
                device.setCurrentFloor(null);
                deviceRepository.save(device);
            } else {
                // Non-serial: cộng kho về warehouse
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
                    toStock.setQuantity(qty);
                } else {
                    toStock.setQuantity(toStock.getQuantity() + qty);
                }
                deviceWarehouseRepository.save(toStock);
            }
        }
    }

    @Override
    public AssetHandoverResponse getAssetHandoverForm(Integer id) {
        AssetTransaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, id));

        if (transaction.getTransactionType() != TransactionType.RETURN_FROM_USER) {
            throw new CustomException(ErrorCode.TRANSACTION_NOT_FOUND);
        }
        return assignmentMapper.toAssetHandoverResponse(transaction);
    }
}