package com.concentrix.asset.service.impl.transaction;

import com.concentrix.asset.dto.request.CreateTransferRequest;
import com.concentrix.asset.dto.response.TransferResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.enums.TransactionStatus;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.TransferMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.transaction.TransferService;
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
    WarehouseRepository warehouseRepository;

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

        Warehouse fromWarehouse = warehouseRepository.findById(request.getFromWarehouseId()).orElseThrow(
                () -> new CustomException(ErrorCode.WAREHOUSE_NOT_FOUND, request.getFromWarehouseId()));

        Warehouse toWarehouse = warehouseRepository.findById(request.getToWarehouseId()).orElseThrow(
                () -> new CustomException(ErrorCode.WAREHOUSE_NOT_FOUND, request.getToWarehouseId()));

        if (fromWarehouse.getSite().getSiteId()
                .equals(toWarehouse.getSite().getSiteId())) {
            throw new CustomException(ErrorCode.INVALID_SITE_TRANSFER);
        }

        // Gom các serialNumber không tìm thấy vào một list
        List<String> serialNotFound = new java.util.ArrayList<>();
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

                    TransactionDetail detail = new TransactionDetail();
                    detail.setDevice(device);
                    detail.setQuantity(item.getQuantity());
                    detail.setTransaction(finalTransaction);
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
    public Page<TransferResponse> filterTransfers(String search, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }

        return transactionRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("transactionType"), TransactionType.TRANSFER_SITE));
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate fromWarehouseName = cb.like(cb.lower(root.get("fromWarehouse").get("warehouseName")), searchPattern);
                Predicate toWarehouseName = cb.like(cb.lower(root.get("toWarehouse").get("warehouseName")), searchPattern);
                predicates.add(cb.or(fromWarehouseName, toWarehouseName));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable).map(transferMapper::toTransferResponse);
    }

    @Override
    public Page<TransferResponse> filterTransfersSitePending(Pageable pageable) {
        return transactionRepository.findAllByTransactionTypeAndTransactionStatus(
                TransactionType.TRANSFER_SITE, TransactionStatus.PENDING, pageable)
                .map(transferMapper::toTransferResponse);
    }

    private User getCurrentUser() {
        String EID = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));
    }

    private void updateDeviceAndWarehousesForTransfer(AssetTransaction transaction) {
        // Gom các serial invalid vào list
        java.util.List<String> serialInvalid = new java.util.ArrayList<>();
        for (TransactionDetail detail : transaction.getDetails()) {
            Device device = detail.getDevice();
            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
            if (hasSerial) {
                // Bổ sung kiểm tra device có đúng ở warehouse không
                if (device.getCurrentWarehouse() == null || !device.getCurrentWarehouse().getWarehouseId()
                        .equals(transaction.getFromWarehouse().getWarehouseId())) {
                    serialInvalid.add(device.getSerialNumber());
                    continue;
                }
                device.setStatus(DeviceStatus.ON_THE_MOVE);
                device.setCurrentWarehouse(null);
                device.setCurrentFloor(null);
                device.setCurrentUser(null);
                deviceRepository.save(device);
            } else {
                Integer deviceId = device.getDeviceId();
                Integer fromWarehouseId = transaction.getFromWarehouse().getWarehouseId();
                Integer qty = detail.getQuantity();
                DeviceWarehouse fromStock = deviceWarehouseRepository
                        .findByWarehouse_WarehouseIdAndDevice_DeviceId(fromWarehouseId, deviceId)
                        .orElse(null);
                if (fromStock == null) {
                    throw new CustomException(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE,
                            device.getModel().getModelName(),
                            transaction.getFromWarehouse().getWarehouseName());
                }
                if (fromStock.getQuantity() < qty) {
                    throw new CustomException(ErrorCode.STOCK_OUT, device.getModel().getModelName());
                }
                fromStock.setQuantity(fromStock.getQuantity() - qty);
                deviceWarehouseRepository.save(fromStock);
                // Không cộng vào toWarehouse ở bước tạo transfer
            }
        }
        if (!serialInvalid.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_DEVICE_STATUS, String.join(",", serialInvalid));
        }
    }
}
