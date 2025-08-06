package com.concentrix.asset.service.impl.transaction;

import com.concentrix.asset.dto.request.CreateReturnFromRepairRequest;
import com.concentrix.asset.dto.response.ReturnFromRepairResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.ReturnFromRepairMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.transaction.ReturnFromRepairService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ReturnFromRepairServiceImpl implements ReturnFromRepairService {
    TransactionRepository transactionRepository;
    ReturnFromRepairMapper returnFromRepairMapper;
    UserRepository userRepository;
    DeviceRepository deviceRepository;
    DeviceWarehouseRepository deviceWarehouseRepository;
    TransactionDetailRepository transactionDetailRepository;

    @Override
    public ReturnFromRepairResponse getReturnFromRepairById(Integer returnId) {
        AssetTransaction transaction = transactionRepository.findById(returnId).orElseThrow(
                () -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, returnId));
        return returnFromRepairMapper.toReturnFromRepairResponse(transaction);
    }

    @Override
    public ReturnFromRepairResponse createReturnFromRepair(CreateReturnFromRepairRequest request) {
        AssetTransaction transaction = returnFromRepairMapper.toAssetTransaction(request);
        transaction.setCreatedBy(getCurrentUser());

        // Hợp nhất các item trùng serialNumber hoặc modelId
        java.util.Map<String, Integer> serialQtyMap = new java.util.HashMap<>();
        java.util.Map<Integer, Integer> modelQtyMap = new java.util.HashMap<>();

        for (var item : request.getItems()) {
            if (item.getSerialNumber() != null && !item.getSerialNumber().isEmpty()) {
                serialQtyMap.merge(item.getSerialNumber(), item.getQuantity(), Integer::sum);
            } else if (item.getModelId() != null) {
                modelQtyMap.merge(item.getModelId(), item.getQuantity(), Integer::sum);
            }
        }

        AssetTransaction finalTransaction = transaction;
        java.util.List<TransactionDetail> details = new java.util.ArrayList<>();
        java.util.List<String> serialNotFound = new java.util.ArrayList<>();
        java.util.List<String> serialInvalid = new java.util.ArrayList<>();

        // Xử lý các device có serial number
        for (java.util.Map.Entry<String, Integer> entry : serialQtyMap.entrySet()) {
            Device device = deviceRepository.findBySerialNumber(entry.getKey())
                    .orElse(null);
            if (device == null) {
                serialNotFound.add(entry.getKey());
                continue;
            }
            // Nếu có serial, kiểm tra transaction cuối cùng phải là REPAIR
            TransactionDetail lastDetail = transactionDetailRepository
                    .findFirstByDevice_DeviceIdOrderByTransaction_TransactionIdDesc(device.getDeviceId());
            if (lastDetail == null || lastDetail.getTransaction() == null || lastDetail.getTransaction()
                    .getTransactionType() != com.concentrix.asset.enums.TransactionType.REPAIR) {
                serialInvalid.add(device.getSerialNumber());
                continue;
            }
            TransactionDetail detail = new TransactionDetail();
            detail.setDevice(device);
            detail.setQuantity(entry.getValue());
            detail.setTransaction(finalTransaction);
            details.add(detail);
        }

        // Xử lý các device theo modelId
        for (java.util.Map.Entry<Integer, Integer> entry : modelQtyMap.entrySet()) {
            Device device = deviceRepository.findFirstByModel_ModelId(entry.getKey())
                    .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND, "Model ID: " + entry.getKey()));
            TransactionDetail detail = new TransactionDetail();
            detail.setDevice(device);
            detail.setQuantity(entry.getValue());
            detail.setTransaction(finalTransaction);
            details.add(detail);
        }

        if (!serialNotFound.isEmpty()) {
            throw new CustomException(ErrorCode.DEVICE_NOT_FOUND, String.join(",", serialNotFound));
        }
        if (!serialInvalid.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_DEVICE_STATUS, String.join(",", serialInvalid));
        }

        transaction.setDetails(details);

        transaction = transactionRepository.save(transaction);
        updateWarehouses(transaction);

        return returnFromRepairMapper.toReturnFromRepairResponse(transaction);
    }

    @Override
    public Page<ReturnFromRepairResponse> filterReturnFromRepairs(Integer transactionId, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
        return transactionRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("transactionType"), TransactionType.RETURN_FROM_REPAIR));
            if (transactionId != null) {
                predicates.add(cb.equal(root.get("transactionId"), transactionId));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable).map(returnFromRepairMapper::toReturnFromRepairResponse);
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
                // Chuẩn hóa: chỉ update Device, không động vào DeviceWarehouse
                device.setStatus(com.concentrix.asset.enums.DeviceStatus.IN_STOCK);
                device.setCurrentWarehouse(transaction.getToWarehouse());
                device.setCurrentUser(null);
                device.setCurrentFloor(null);
                deviceRepository.save(device);
            } else {
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
}