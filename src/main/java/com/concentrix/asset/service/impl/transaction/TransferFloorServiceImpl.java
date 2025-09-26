package com.concentrix.asset.service.impl.transaction;

import com.concentrix.asset.dto.request.CreateTransferFloorRequest;
import com.concentrix.asset.dto.response.TransferFloorResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.TransferFloorMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.DeviceService;
import com.concentrix.asset.service.transaction.TransferFloorService;
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
import java.util.*;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class TransferFloorServiceImpl implements TransferFloorService {
    TransactionRepository transactionRepository;
    TransferFloorMapper transferFloorMapper;
    DeviceRepository deviceRepository;
    FloorRepository floorRepository;
    UserRepository userRepository;
    DeviceService deviceService;
    DeviceFloorRepository deviceFloorRepository;

    @Override
    public TransferFloorResponse getTransferFloorById(Integer transferFloorId) {
        AssetTransaction transaction = transactionRepository.findById(transferFloorId).orElseThrow(
                () -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, transferFloorId));
        return transferFloorMapper.toTransferFloorResponse(transaction);
    }

    @Override
    public TransferFloorResponse createTransferFloor(CreateTransferFloorRequest request) {

        Floor fromFloor = floorRepository.findById(request.getFromFloorId())
                .orElseThrow(() -> new CustomException(ErrorCode.FLOOR_NOT_FOUND, request.getFromFloorId()));

        Floor toFloor = floorRepository.findById(request.getToFloorId())
                .orElseThrow(() -> new CustomException(ErrorCode.FLOOR_NOT_FOUND, request.getToFloorId()));

        if (fromFloor.getFloorId().equals(toFloor.getFloorId())
                || !fromFloor.getSite().getSiteId().equals(toFloor.getSite().getSiteId())) {
            throw new CustomException(ErrorCode.INVALID_FLOOR_TRANSFER);
        }


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


        // Gom các serial not found/invalid vào list
        List<String> serialNotFound = new ArrayList<>();
        List<String> serialInvalid = new ArrayList<>();
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
                        // Serial: chỉ cho phép chuyển sàn nếu đang IN_FLOOR
                        if (device.getStatus() != DeviceStatus.IN_FLOOR) {
                            serialInvalid.add(device.getSerialNumber());
                            return null;
                        }
                        // Bổ sung kiểm tra device có đúng ở floor không
                        if (device.getCurrentFloor() == null || !device.getCurrentFloor().getFloorId()
                                .equals(finalTransaction.getFromFloor().getFloorId())) {
                            serialInvalid.add(device.getSerialNumber());
                            return null;
                        }
                    }
                    TransactionDetail detail = new TransactionDetail();
                    detail.setDevice(device);
                    detail.setQuantity(item.getQuantity());
                    detail.setTransaction(finalTransaction);
                    return detail;
                })
                .filter(Objects::nonNull)
                .toList();

        if (!serialNotFound.isEmpty()) {
            throw new CustomException(ErrorCode.DEVICE_NOT_FOUND, String.join(",", serialNotFound));
        }
        if (!serialInvalid.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_DEVICE_STATUS, String.join(",", serialInvalid));
        }

        transaction.setDetails(details);

        transaction = transactionRepository.save(transaction);
        updateForTransferFloor(transaction);

        return transferFloorMapper.toTransferFloorResponse(transaction);
    }

    @Override
    public Page<TransferFloorResponse> filterTransferFloors(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
        return transactionRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("transactionType"), TransactionType.TRANSFER_FLOOR));
            if (search != null && !search.isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fromFloor").get("floorName")), searchPattern),
                        cb.like(cb.lower(root.get("toFloor").get("floorName")), searchPattern),
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
        }, pageable).map(transferFloorMapper::toTransferFloorResponse);
    }

    private User getCurrentUser() {
        String EID = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));
    }

    // Cập nhật trạng thái thiết bị sau khi chuyển sàn
    private void updateForTransferFloor(AssetTransaction transaction) {
        for (TransactionDetail detail : transaction.getDetails()) {
            Device device = detail.getDevice();
            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
            if (hasSerial) {
                device.setCurrentFloor(transaction.getToFloor());
                device.setHostName(deviceService.generateHostNameForDesktop(device, transaction.getToFloor()));
                device.setSeatNumber(null);
                deviceRepository.save(device);
            } else {
                DeviceFloor deviceFloorTo = deviceFloorRepository
                        .findByDevice_DeviceIdAndFloor_FloorId(device.getDeviceId(), transaction.getToFloor().getFloorId())
                        .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND_IN_FLOOR, device.getModel().getModelName()));

                if (deviceFloorTo.getQuantity() < detail.getQuantity()) {
                    throw new CustomException(ErrorCode.DEVICE_NOT_ENOUGH_IN_FLOOR, device.getModel().getModelName(), deviceFloorTo.getFloor().getFloorName());
                } else {
                    deviceFloorTo.setQuantity(deviceFloorTo.getQuantity() - detail.getQuantity());
                }

                DeviceFloor deviceFloorFrom = deviceFloorRepository
                        .findByDevice_DeviceIdAndFloor_FloorId(device.getDeviceId(), transaction.getFromFloor().getFloorId())
                        .orElseGet(() -> DeviceFloor.builder()
                                .device(device)
                                .floor(transaction.getFromFloor())
                                .quantity(0)
                                .build()
                        );
                deviceFloorFrom.setQuantity(deviceFloorFrom.getQuantity() + detail.getQuantity());
                deviceFloorRepository.save(deviceFloorFrom);
                deviceFloorRepository.save(deviceFloorTo);
            }
        }
    }
}