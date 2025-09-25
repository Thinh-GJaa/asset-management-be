package com.concentrix.asset.service.impl.transaction;

import com.concentrix.asset.dto.request.CreateDisposalRequest;
import com.concentrix.asset.dto.response.DisposalResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.DisposalMapper;
import com.concentrix.asset.repository.DeviceRepository;
import com.concentrix.asset.repository.DeviceWarehouseRepository;
import com.concentrix.asset.repository.TransactionRepository;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.service.transaction.DisposalService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

import jakarta.persistence.criteria.Predicate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class DisposalServiceImpl implements DisposalService {
    TransactionRepository transactionRepository;
    DisposalMapper disposalMapper;
    UserRepository userRepository;
    DeviceRepository deviceRepository;
    DeviceWarehouseRepository deviceWarehouseRepository;

    @Override
    public DisposalResponse getDisposalById(Integer disposalId) {
        AssetTransaction transaction = transactionRepository.findById(disposalId).orElseThrow(
                () -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, disposalId));
        return disposalMapper.toDisposalResponse(transaction);
    }

    @Override
    public DisposalResponse createDisposal(CreateDisposalRequest request) {
        AssetTransaction transaction = disposalMapper.toAssetTransaction(request);
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

        // Gom các serial not found/invalid vào list
        java.util.List<String> serialNotFound = new java.util.ArrayList<>();
        java.util.List<String> serialInvalid = new java.util.ArrayList<>();
        final AssetTransaction finalTransaction = transaction;
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
                        // Serial: chỉ cho phép disposal nếu đang IN_STOCK
                        if (device.getStatus() != DeviceStatus.IN_STOCK) {
                            serialInvalid.add(device.getSerialNumber());
                            return null;
                        }
                        // Bổ sung kiểm tra device có đúng ở warehouse không
                        if (device.getCurrentWarehouse() == null || !device.getCurrentWarehouse().getWarehouseId()
                                .equals(finalTransaction.getFromWarehouse().getWarehouseId())) {
                            serialInvalid.add(device.getSerialNumber());
                            return null;
                        }
                    } else {
                        // Non-serial: kiểm tra tồn kho trước khi disposal
                        Integer fromWarehouseId = finalTransaction.getFromWarehouse().getWarehouseId();
                        Integer qty = item.getQuantity();
                        DeviceWarehouse fromStock = deviceWarehouseRepository
                                .findByWarehouse_WarehouseIdAndDevice_DeviceId(fromWarehouseId, device.getDeviceId())
                                .orElse(null);
                        if (fromStock == null) {
                            throw new CustomException(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE,
                                    device.getModel().getModelName(),
                                    finalTransaction.getFromWarehouse().getWarehouseName());
                        }
                        if (fromStock.getQuantity() < qty) {
                            throw new CustomException(ErrorCode.STOCK_OUT, device.getModel().getModelName(), fromStock.getQuantity());
                        }
                    }
                    TransactionDetail detail = new TransactionDetail();
                    detail.setDevice(device);
                    detail.setQuantity(item.getQuantity());
                    detail.setTransaction(finalTransaction);
                    return detail;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!serialNotFound.isEmpty()) {
            throw new CustomException(ErrorCode.DEVICE_NOT_FOUND, String.join(",", serialNotFound));
        }
        if (!serialInvalid.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_DEVICE_STATUS, String.join(",", serialInvalid));
        }

        transaction.setDetails(details);

        transaction = transactionRepository.save(transaction);
        updateStockForDisposal(transaction);

        return disposalMapper.toDisposalResponse(transaction);
    }

    @Override
    public Page<DisposalResponse> filterDisposals(String search, LocalDate fromDate, LocalDate toDate,
                                                  Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
        return transactionRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("transactionType"), TransactionType.DISPOSAL));

            if (search != null && !search.trim().isEmpty()) {
                String keyword = "%" + search.trim().toLowerCase() + "%";

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("createdBy").get("fullName")), keyword),
                        cb.like(cb.lower(root.get("fromWarehouse").get("warehouseName")), keyword)));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }

            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate.atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable).map(disposalMapper::toDisposalResponse);
    }

    private User getCurrentUser() {
        String EID = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));
    }

    private void updateStockForDisposal(AssetTransaction transaction) {
        for (TransactionDetail detail : transaction.getDetails()) {
            Device device = detail.getDevice();

            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();

            if (hasSerial) {
                device.setStatus(DeviceStatus.DISPOSED);
                device.setCurrentWarehouse(null);
                device.setCurrentUser(null);
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
                    throw new CustomException(ErrorCode.STOCK_OUT, device.getModel().getModelName(), fromStock.getQuantity());
                }
                fromStock.setQuantity(fromStock.getQuantity() - qty);
                deviceWarehouseRepository.save(fromStock);
            }
        }
    }
}