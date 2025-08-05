package com.concentrix.asset.service.impl.transaction;

import java.time.LocalDateTime;

import com.concentrix.asset.dto.request.CreateReturnFromFloorRequest;
import com.concentrix.asset.dto.response.ReturnFromFloorResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.ReturnFromFloorMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.transaction.ReturnFromFloorService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReturnFromFloorServiceImpl implements ReturnFromFloorService {
    TransactionRepository transactionRepository;
    ReturnFromFloorMapper returnFromFloorMapper;
    DeviceRepository deviceRepository;
    DeviceWarehouseRepository deviceWarehouseRepository;
    FloorRepository floorRepository;
    UserRepository userRepository;
    TransactionDetailRepository transactionDetailRepository;

    @Override
    public ReturnFromFloorResponse getReturnFromFloorById(Integer returnFromFloorId) {
        AssetTransaction transaction = transactionRepository.findById(returnFromFloorId).orElseThrow(
                () -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, returnFromFloorId));
        return returnFromFloorMapper.toReturnFromFloorResponse(transaction);
    }

    @Override
    public ReturnFromFloorResponse createReturnFromFloor(CreateReturnFromFloorRequest request) {
        AssetTransaction transaction = returnFromFloorMapper.toAssetTransaction(request);
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
                    throw new CustomException(ErrorCode.DUPLICATE_SERIAL_NUMBER, item.getModelId());
                }
            }
        }

        final AssetTransaction finalTransaction = transaction;
        List<String> serialNotFound = new ArrayList<>();
        List<String> serialInvalid = new ArrayList<>();

        List<TransactionDetail> details = request.getItems().stream()
                .map(item -> {
                    Device device = null;

                    // Trường hợp có serialNumber
                    if (item.getSerialNumber() != null && !item.getSerialNumber().isEmpty()) {
                        Optional<Device> optionalDevice = deviceRepository.findBySerialNumber(item.getSerialNumber());
                        if (optionalDevice.isEmpty()) {
                            serialNotFound.add(item.getSerialNumber());
                            return null;
                        }

                        device = optionalDevice.get();

                        // Kiểm tra trạng thái thiết bị phải là IN_FLOOR
                        if (device.getStatus() != DeviceStatus.IN_FLOOR) {
                            serialInvalid.add(device.getSerialNumber());
                            return null;
                        }

                        // Kiểm tra thiết bị có ở đúng floor hay không
                        if (device.getCurrentFloor() == null ||
                                !device.getCurrentFloor().getFloorId().equals(finalTransaction.getFromFloor().getFloorId())) {
                            serialInvalid.add(device.getSerialNumber());
                            return null;
                        }

                    } else if (item.getModelId() != null) {
                        // Trường hợp không có serial nhưng có modelId
                        Optional<Device> optionalDevice = deviceRepository.findFirstByModel_ModelId(item.getModelId());
                        if (optionalDevice.isEmpty()) {
                            serialNotFound.add("Model ID: " + item.getModelId());
                            return null;
                        }

                        device = optionalDevice.get();
                    } else {
                        // Không có serial và không có modelId => bỏ qua
                        return null;
                    }

                    // Tạo TransactionDetail
                    TransactionDetail detail = new TransactionDetail();
                    detail.setDevice(device);
                    detail.setQuantity(item.getQuantity());
                    detail.setTransaction(finalTransaction);
                    return detail;

                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Nếu có serialNumber không tìm thấy hoặc invalid thì trả về list
        if (!serialNotFound.isEmpty())
            throw new CustomException(ErrorCode.DEVICE_NOT_FOUND,
                    String.join(",", serialNotFound));

        if (!serialInvalid.isEmpty())
            throw new CustomException(ErrorCode.INVALID_DEVICE_STATUS,
                    String.join(",", serialInvalid));

        transaction.setDetails(details);

        transaction = transactionRepository.save(transaction);
        updateStockForReturnFromFloor(transaction);

        return returnFromFloorMapper.toReturnFromFloorResponse(transaction);
    }

    @Override
    public Page<ReturnFromFloorResponse> filterReturnFromFloors(Integer transactionId, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
        return transactionRepository.findALLByTransactionTypeAndDynamicFilter(
                TransactionType.RETURN_FROM_FLOOR, transactionId, fromDate, toDate, pageable)
                .map(returnFromFloorMapper::toReturnFromFloorResponse);
    }

    private User getCurrentUser() {
        String EID = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));
    }

    private void updateStockForReturnFromFloor(AssetTransaction transaction) {
        for (TransactionDetail detail : transaction.getDetails()) {
            Device device = detail.getDevice();
            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
            if (hasSerial) {
                // Serial: chỉ update Device, không động vào DeviceWarehouse
                device.setStatus(DeviceStatus.IN_STOCK);
                device.setCurrentWarehouse(transaction.getToWarehouse());
                device.setCurrentFloor(null);
                deviceRepository.save(device);
            } else {
                Integer deviceId = device.getDeviceId();
                Integer toWarehouseId = transaction.getToWarehouse().getWarehouseId();
                Integer qty = detail.getQuantity();
                DeviceWarehouse fromStock = deviceWarehouseRepository
                        .findByWarehouse_WarehouseIdAndDevice_DeviceId(toWarehouseId, deviceId)
                        .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE,
                                device.getModel().getModelName(),
                                transaction.getFromFloor().getFloorName()));
                if (fromStock.getQuantity() < qty) {
                    throw new CustomException(ErrorCode.STOCK_OUT, device.getModel().getModelName());
                }
                fromStock.setQuantity(fromStock.getQuantity() - qty);
                deviceWarehouseRepository.save(fromStock);
                // Cộng về kho
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