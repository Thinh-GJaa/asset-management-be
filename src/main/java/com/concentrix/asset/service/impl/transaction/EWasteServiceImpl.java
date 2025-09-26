package com.concentrix.asset.service.impl.transaction;

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
import com.concentrix.asset.service.transaction.EWasteService;
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
import java.util.List;
import java.util.Set;
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
                        // Serial: chỉ cho phép E-Waste nếu đang IN_STOCK
                        if (device.getStatus() != com.concentrix.asset.enums.DeviceStatus.IN_STOCK) {
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
                        // Non-serial: kiểm tra tồn kho trước khi cho E-Waste
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
                .filter(detail -> detail != null)
                .collect(Collectors.toList());

        if (!serialNotFound.isEmpty()) {
            throw new CustomException(ErrorCode.DEVICE_NOT_FOUND, String.join(",", serialNotFound));
        }
        if (!serialInvalid.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_DEVICE_STATUS, String.join(",", serialInvalid));
        }

        transaction.setDetails(details);

        transaction = transactionRepository.save(transaction);
        updateStockForEWaste(transaction);

        return ewasteMapper.toEWasteResponse(transaction);
    }

    @Override
    public Page<EWasteResponse> filterEWastes(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
        return transactionRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("transactionType"), TransactionType.E_WASTE));
            if (search != null && !search.trim().isEmpty()) {
                String keyword = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fromWarehouse").get("warehouseName")), keyword),
                        cb.like(cb.lower(root.get("createdBy").get("fullName")), keyword)
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
        }, pageable).map(ewasteMapper::toEWasteResponse);
    }

    private User getCurrentUser() {
        String EID = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));
    }

    private void updateStockForEWaste(AssetTransaction transaction) {
        for (TransactionDetail detail : transaction.getDetails()) {
            Device device = detail.getDevice();
            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
            if (hasSerial) {
                // Serial: chỉ update Device, không động vào DeviceWarehouse
                device.setStatus(com.concentrix.asset.enums.DeviceStatus.E_WASTE);
                device.setCurrentWarehouse(transaction.getFromWarehouse()); // Cập nhật kho hiện tại
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