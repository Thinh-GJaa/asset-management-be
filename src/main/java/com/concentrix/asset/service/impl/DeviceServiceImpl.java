package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.UpdateDeviceRequest;
import com.concentrix.asset.dto.request.UpdateSeatNumberRequest;
import com.concentrix.asset.dto.response.DeviceResponse;
import com.concentrix.asset.dto.response.DeviceMovementHistoryResponse;
import com.concentrix.asset.dto.response.DeviceBorrowingInfoResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.DeviceMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.DeviceService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;

import com.concentrix.asset.enums.TransactionType;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class DeviceServiceImpl implements DeviceService {

    DeviceRepository deviceRepository;
    DeviceMapper deviceMapper;
    ModelRepository modelRepository;
    TransactionDetailRepository transactionDetailRepository;
    PODetailRepository poDetailRepository;
    TransactionRepository transactionRepository;

    @Override
    public DeviceResponse getDeviceById(Integer deviceId) {
        return deviceRepository.findById(deviceId)
                .map(deviceMapper::toDeviceResponse)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND, deviceId));
    }

    @Override
    public DeviceResponse updateDevice(UpdateDeviceRequest request) {

        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND, request.getDeviceId()));

        modelRepository.findById(request.getModelId())
                .orElseThrow(() -> new CustomException(ErrorCode.MODEL_NOT_FOUND, request.getModelId()));

        if (request.getSerialNumber() != null && !request.getSerialNumber().isEmpty()) {
            if (deviceRepository.findBySerialNumber(request.getSerialNumber()).isPresent()
                    && !device.getSerialNumber().equals(request.getSerialNumber())) {
                throw new CustomException(ErrorCode.SERIAL_NUMBER_ALREADY_EXISTS, request.getSerialNumber());
            }
        }

        deviceMapper.updateDevice(device, request);
        device = deviceRepository.save(device);
        log.info("[DeviceServiceImpl] Updated device with ID: {}", device.getDeviceId());
        return deviceMapper.toDeviceResponse(device);

    }

    @Override
    public Page<DeviceResponse> filterDevices(String search, DeviceType type, Integer modelId, DeviceStatus status,
            Pageable pageable) {
        return deviceRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("deviceName")), like),
                        cb.like(cb.lower(root.get("serialNumber")), like)));
            }

            if (type != null) {
                predicates.add(cb.equal(root.get("model").get("type"), type));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (modelId != null) {
                predicates.add(cb.equal(root.get("model").get("modelId"), modelId));
            }
            predicates.add(cb.isNotNull(root.get("serialNumber")));

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable).map(deviceMapper::toDeviceResponse);
    }

    @Override
    public List<DeviceMovementHistoryResponse> getDeviceMovementHistoryBySerial(String serialNumber) {
        Device device = deviceRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND, serialNumber));
        final LocalDate purchaseDate;
        PODetail poDetail = poDetailRepository.findByDevice_DeviceIdAndDevice_SerialNumberNotNull(device.getDeviceId());
        if (poDetail != null && poDetail.getPurchaseOrder() != null) {
            purchaseDate = poDetail.getPurchaseOrder().getCreatedAt();
        } else {
            purchaseDate = null;
        }
        List<TransactionDetail> details = transactionDetailRepository
                .findAllByDevice_DeviceIdOrderByTransaction_TransactionIdAsc(device.getDeviceId());
        DeviceMovementHistoryResponse.UserResponse poUser = null;
        if (poDetail != null && poDetail.getPurchaseOrder() != null
                && poDetail.getPurchaseOrder().getCreatedBy() != null) {
            poUser = DeviceMovementHistoryResponse.UserResponse.builder()
                    .eid(poDetail.getPurchaseOrder().getCreatedBy().getEid())
                    .fullName(poDetail.getPurchaseOrder().getCreatedBy().getFullName())
                    .build();
        }
        List<DeviceMovementHistoryResponse> history = details.stream()
                .map(detail -> detail.getTransaction())
                .map(tx -> DeviceMovementHistoryResponse.builder()
                        .description(buildTransactionDescription(tx))
                        .createdAt(tx.getCreatedAt())
                        .createdBy(tx.getCreatedBy() != null ? DeviceMovementHistoryResponse.UserResponse.builder()
                                .eid(tx.getCreatedBy().getEid())
                                .fullName(tx.getCreatedBy().getFullName())
                                .build() : null)
                        .build())
                .collect(Collectors.toList());
        // Bổ sung PO movement đầu tiên nếu có
        if (poDetail != null && poDetail.getPurchaseOrder() != null) {
            PurchaseOrder po = poDetail.getPurchaseOrder();
            String poDesc = "Purchased from vendor " + (po.getVendor() != null ? po.getVendor().getVendorName() : "")
                    + " for warehouse " + (po.getWarehouse() != null ? po.getWarehouse().getWarehouseName() : "");
            DeviceMovementHistoryResponse poMovement = DeviceMovementHistoryResponse.builder()
                    .description(poDesc)
                    .createdAt(po.getCreatedAt() != null ? po.getCreatedAt().atStartOfDay() : null)
                    .createdBy(poUser)
                    .build();
            history.add(0, poMovement);
        }
        return history;
    }

    @Override
    public List<DeviceBorrowingInfoResponse> getAllUserBorrowingDevices() {
        List<Device> allDevices = deviceRepository.findAll();
        Map<String, DeviceBorrowingInfoResponse> userMap = new HashMap<>();
        for (Device device : allDevices) {
            boolean hasSerial = device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
            List<TransactionDetail> allDetails = transactionDetailRepository
                    .findAllByDevice_DeviceIdOrderByTransaction_TransactionIdAsc(device.getDeviceId());
            if (hasSerial) {
                // Xử lý như cũ cho serial
                TransactionDetail lastDetail = allDetails.isEmpty() ? null : allDetails.get(allDetails.size() - 1);
                if (lastDetail == null)
                    continue;
                AssetTransaction lastTx = lastDetail.getTransaction();
                if (lastTx == null)
                    continue;
                if (lastTx.getTransactionType() == TransactionType.ASSIGNMENT && lastTx.getUserUse() != null) {
                    boolean returned = false;
                    for (TransactionDetail detail : allDetails) {
                        AssetTransaction tx = detail.getTransaction();
                        if (tx.getTransactionId() > lastTx.getTransactionId()
                                && tx.getTransactionType() == TransactionType.RETURN_FROM_USER) {
                            returned = true;
                            break;
                        }
                    }
                    if (!returned) {
                        User user = lastTx.getUserUse();
                        String eid = user.getEid();
                        DeviceBorrowingInfoResponse.DeviceInfo deviceInfo = DeviceBorrowingInfoResponse.DeviceInfo
                                .builder()
                                .serialNumber(device.getSerialNumber())
                                .deviceName(device.getDeviceName())
                                .quantity(1)
                                .modelId(
                                        device.getModel() != null
                                                ? device.getModel().getModelId()
                                                : null)
                                .modelName(device.getModel() != null ? device.getModel().getModelName() : null)
                                .build();
                        DeviceBorrowingInfoResponse info = userMap.get(eid);
                        if (info == null) {
                            info = DeviceBorrowingInfoResponse.builder()
                                    .eid(user.getEid())
                                    .fullName(user.getFullName())
                                    .devices(new java.util.ArrayList<>())
                                    .build();
                            userMap.put(eid, info);
                        }
                        info.getDevices().add(deviceInfo);
                    }
                }
            } else {
                // Gom theo user, tính tổng ASSIGNMENT - RETURN_FROM_USER
                Map<String, Integer> assignMap = new HashMap<>();
                Map<String, LocalDateTime> assignTimeMap = new HashMap<>();
                for (TransactionDetail detail : allDetails) {
                    AssetTransaction tx = detail.getTransaction();
                    if (tx.getUserUse() == null)
                        continue;
                    String eid = tx.getUserUse().getEid();
                    if (tx.getTransactionType() == TransactionType.ASSIGNMENT) {
                        assignMap.put(eid, assignMap.getOrDefault(eid, 0) + detail.getQuantity());
                        // Lưu thời gian cấp phát gần nhất
                        assignTimeMap.put(eid, tx.getCreatedAt());
                    } else if (tx.getTransactionType() == TransactionType.RETURN_FROM_USER) {
                        assignMap.put(eid, assignMap.getOrDefault(eid, 0) - detail.getQuantity());
                    }
                }
                for (Map.Entry<String, Integer> entry : assignMap.entrySet()) {
                    if (entry.getValue() != null && entry.getValue() > 0) {
                        String eid = entry.getKey();
                        User user = allDetails.stream().map(TransactionDetail::getTransaction)
                                .filter(tx -> tx.getUserUse() != null && tx.getUserUse().getEid().equals(eid))
                                .map(AssetTransaction::getUserUse).findFirst().orElse(null);
                        if (user == null)
                            continue;
                        DeviceBorrowingInfoResponse.DeviceInfo deviceInfo = DeviceBorrowingInfoResponse.DeviceInfo
                                .builder()
                                .serialNumber(device.getSerialNumber())
                                .deviceName(device.getDeviceName())
                                .quantity(entry.getValue())
                                .modelId(
                                        device.getModel() != null
                                                ? device.getModel().getModelId() == null ? null
                                                        : device.getModel().getModelId()
                                                : null)
                                .modelName(device.getModel() != null ? device.getModel().getModelName() : null)
                                .build();
                        DeviceBorrowingInfoResponse info = userMap.get(eid);
                        if (info == null) {
                            info = DeviceBorrowingInfoResponse.builder()
                                    .eid(user.getEid())
                                    .fullName(user.getFullName())
                                    .devices(new java.util.ArrayList<>())
                                    .build();
                            userMap.put(eid, info);
                        }
                        info.getDevices().add(deviceInfo);
                    }
                }
            }
        }
        return new java.util.ArrayList<>(userMap.values());
    }

    @Override
    public List<DeviceBorrowingInfoResponse.DeviceInfo> getBorrowingDevicesByUser(String eid) {

        List<DeviceBorrowingInfoResponse.DeviceInfo> result = new java.util.ArrayList<>();

        List<Device> devicesByEid = deviceRepository.findAllByCurrentUser_Eid(eid);

        for (Device dv : devicesByEid) {
            if (dv.getSerialNumber() != null && !dv.getSerialNumber().isEmpty()) {
                DeviceBorrowingInfoResponse.DeviceInfo deviceInfo = DeviceBorrowingInfoResponse.DeviceInfo.builder()
                        .serialNumber(dv.getSerialNumber())
                        .deviceName(dv.getDeviceName())
                        .quantity(1)
                        .modelId(
                                dv.getModel() != null
                                        ? dv.getModel().getModelId() == null ? null
                                                : dv.getModel().getModelId()
                                        : null)
                        .modelName(dv.getModel() != null ? dv.getModel().getModelName() : null)
                        .build();
                result.add(deviceInfo);
            }
        }

        List<Object[]> devicesWithoutSerial = transactionDetailRepository.getDeviceAndQuantityByEid(eid);
        log.info("[DeviceServiceImpl] Found {}  serial for user {}", devicesWithoutSerial, eid);
        for (Object[] obj : devicesWithoutSerial) {
            Device dv = (Device) obj[0];
            Integer quantity = ((Long) obj[1]).intValue();

            if (quantity != 0) {
                DeviceBorrowingInfoResponse.DeviceInfo deviceInfo = DeviceBorrowingInfoResponse.DeviceInfo.builder()
                        .serialNumber(dv.getSerialNumber())
                        .deviceName(dv.getDeviceName())
                        .modelId(dv.getModel().getModelId())
                        .modelName(dv.getModel().getModelName())
                        .quantity(quantity)
                        .build();
                result.add(deviceInfo);
            }
        }
        return result;
    }

    @Override
    public Page<DeviceBorrowingInfoResponse> getBorrowingDevice(Pageable pageable) {
        List<User> users = transactionRepository.findDistinctEidFromTransactions();

        List<DeviceBorrowingInfoResponse> result = users.parallelStream().map(
                user -> {

                    List<DeviceBorrowingInfoResponse.DeviceInfo> deviceInfos = getBorrowingDevicesByUser(user.getEid());

                    if (deviceInfos.isEmpty()) {
                        return null;
                    }

                    return DeviceBorrowingInfoResponse.builder()
                            .eid(user.getEid())
                            .fullName(user.getFullName())
                            .build();
                }).filter(Objects::nonNull).toList();

        return new PageImpl<>(result, pageable, result.size());
    }

    @Override
    public List<String> getAllDeviceTypes() {
        return java.util.Arrays.stream(com.concentrix.asset.enums.DeviceType.values())
                .map(Enum::name)
                .toList();
    }

    @Override
    public List<String> getDeviceStatuses() {
        return java.util.Arrays.stream(com.concentrix.asset.enums.DeviceStatus.values())
                .map(Enum::name)
                .toList();
    }

    @Override
    public String generateHostNameForLaptop(Device device) {
        DeviceType type = device.getModel().getType();
        int lenSN = device.getSerialNumber().length();
        String serialNumber = device.getModel().getManufacturer().equalsIgnoreCase("dell")
                ? device.getSerialNumber().substring(0, 6)
                : device.getSerialNumber().substring(lenSN - 6, lenSN);
        StringBuilder hostName = new StringBuilder();

        if (type == DeviceType.LAPTOP) {
            hostName.append("VNHCM-LAP");
            hostName.append(serialNumber);
            return hostName.toString();
        } else {
            return null;
        }
    }

    @Override
    public void updateSeatNumber(List<UpdateSeatNumberRequest> request) {

        // Initialize lists to track serials
        List<String> existSeatNumber = new ArrayList<>();
        List<String> notFoundSerials = new ArrayList<>();
        List<String> notValid = new ArrayList<>();

        // List to hold updated seat numbers
        List<Device> deviceUpdateList = new ArrayList<>();

        for (UpdateSeatNumberRequest deviceRequest : request) {

            // Kiểm tra serial number có tồn tại không
            Optional<Device> deviceSerialOpt = deviceRepository.findBySerialNumber(deviceRequest.getSerialNumber());
            if (deviceSerialOpt.isEmpty()) {
                notFoundSerials.add(deviceRequest.getSerialNumber());
                continue; // bỏ qua vòng lặp này, check tiếp cái khác
            }
            Device deviceSerial = deviceSerialOpt.get();

            if (deviceSerial.getStatus() != DeviceStatus.IN_FLOOR) {
                notValid.add(deviceRequest.getSerialNumber());
                continue; // bỏ qua vòng lặp này, check tiếp cái khác
            }

            // Kiểm tra seat number đã tồn tại chưa
            Optional<Device> deviceSeatNumberOpt = deviceRepository.findBySeatNumber(deviceRequest.getSeatNumber());
            if (deviceSeatNumberOpt.isPresent()
                    && !deviceSeatNumberOpt.get().getSerialNumber().equals(deviceRequest.getSerialNumber())) {
                existSeatNumber.add(deviceRequest.getSerialNumber());
                continue; // bỏ qua vòng lặp này, check tiếp cái khác
            }
            // Update seat number
            deviceSerial.setSeatNumber(deviceRequest.getSeatNumber());
            deviceUpdateList.add(deviceSerial);
        }

        // Sau vòng lặp, nếu có lỗi thì throw
        if (!notFoundSerials.isEmpty()) {
            throw new CustomException(ErrorCode.DEVICE_NOT_FOUND, String.join(", ", notFoundSerials));
        }

        if (!existSeatNumber.isEmpty()) {
            throw new CustomException(ErrorCode.SEAT_NUMBER_ALREADY_EXISTS, String.join(", ", existSeatNumber));
        }

        if (!notValid.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_DEVICE_STATUS, String.join(", ", notValid));
        }

        // Nếu không có lỗi thì lưu batch
        deviceRepository.saveAll(deviceUpdateList);

    }

    @Override
    public Page<DeviceResponse> filterDevicesNonSeatNumber(String search, Integer siteId, Integer floorId,
            Pageable pageable) {
        return deviceRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isNull(root.get("seatNumber"))); // Chỉ lấy những thiết bị không có seat number
            predicates.add(cb.isNotNull(root.get("serialNumber"))); // Chỉ lấy những thiết bị có serial number
            predicates.add(cb.equal(root.get("status"), DeviceStatus.IN_FLOOR)); // Chỉ lấy những thiết bị có trạng thái
                                                                                 // IN_FLOOR

            if (search != null && !search.trim().isEmpty()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("deviceName")), like),
                        cb.like(cb.lower(root.get("serialNumber")), like),
                        cb.like(cb.lower(root.get("currentFloor").get("floorName")), like),
                        cb.like(cb.lower(root.get("currentFloor").get("site").get("siteName")), like)));
            }

            if (siteId != null) {
                predicates.add(cb.equal(root.get("currentFloor").get("site").get("siteId"), siteId));
            }

            if (floorId != null) {
                predicates.add(cb.equal(root.get("currentFloor").get("floorId"), floorId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable).map(deviceMapper::toDeviceResponse);
    }

    @Override
    public String generateHostNameForDesktop(Device device, Floor floor) {
        DeviceType type = device.getModel().getType();
        int lenSN = device.getSerialNumber().length();
        String serialNumber = device.getModel().getManufacturer().equalsIgnoreCase("dell")
                ? device.getSerialNumber().substring(0, 6)
                : device.getSerialNumber().substring(lenSN - 6, lenSN);
        String accountCode = floor.getAccount().getAccountCode();

        StringBuilder hostName = new StringBuilder();

        if (type != DeviceType.DESKTOP) {
            return null;
        } else {
            if (accountCode.equalsIgnoreCase("gra")) {
                hostName.append("ITVNCNX");
                hostName.append(serialNumber);
                hostName.append("-D");
                return hostName.toString();
            }

            hostName.append("VN");
            switch (floor.getSite().getSiteName().toLowerCase()) {
                case "qtsc1", "qtsc9" -> hostName.append("QUA-");
                case "onehub" -> hostName.append("ONE-");
                case "flemington" -> hostName.append("FLE-");
                case "techvally" -> hostName.append("TEC-");
            }
            hostName.append(accountCode);
            hostName.append(serialNumber);
        }
        return hostName.toString();
    }

    private String buildTransactionDescription(AssetTransaction tx) {
        StringBuilder sb = new StringBuilder();
        switch (tx.getTransactionType()) {
            case ASSIGNMENT:
                sb.append("Assigned to user ");
                if (tx.getUserUse() != null)
                    sb.append(tx.getUserUse().getFullName());
                if (tx.getToWarehouse() != null)
                    sb.append(" from warehouse ").append(tx.getToWarehouse().getWarehouseName());
                break;
            case REPAIR:
                sb.append("Sent for repair at warehouse ");
                if (tx.getToWarehouse() != null)
                    sb.append(tx.getToWarehouse().getWarehouseName());
                break;
            case RETURN_FROM_REPAIR:
                sb.append("Returned from repair to warehouse ");
                if (tx.getToWarehouse() != null)
                    sb.append(tx.getToWarehouse().getWarehouseName());
                break;
            case TRANSFER_SITE:
                sb.append("Transferred from warehouse ");
                if (tx.getFromWarehouse() != null)
                    sb.append(tx.getFromWarehouse().getWarehouseName());
                sb.append(" to warehouse ");
                if (tx.getToWarehouse() != null)
                    sb.append(tx.getToWarehouse().getWarehouseName());
                break;
            case TRANSFER_FLOOR:
                sb.append("Moved from floor ");
                if (tx.getFromFloor() != null)
                    sb.append(tx.getFromFloor().getFloorName());
                sb.append(" to floor ");
                if (tx.getToFloor() != null)
                    sb.append(tx.getToFloor().getFloorName());
                break;
            case USE_FLOOR:
                sb.append("Deployed for use at floor ");
                if (tx.getToFloor() != null)
                    sb.append(tx.getToFloor().getFloorName());
                break;
            case RETURN_FROM_USER:
                sb.append("Returned from user ");
                if (tx.getUserUse() != null)
                    sb.append(tx.getUserUse().getFullName());
                if (tx.getToWarehouse() != null)
                    sb.append(" to warehouse ").append(tx.getToWarehouse().getWarehouseName());
                break;
            case RETURN_FROM_FLOOR:
                sb.append("Returned from floor ");
                if (tx.getFromFloor() != null)
                    sb.append(tx.getFromFloor().getFloorName());
                if (tx.getToWarehouse() != null)
                    sb.append(" to warehouse ").append(tx.getToWarehouse().getWarehouseName());
                break;
            case E_WASTE:
                sb.append("Moved to e-waste at warehouse ");
                if (tx.getToWarehouse() != null)
                    sb.append(tx.getToWarehouse().getWarehouseName());
                break;
            case DISPOSAL:
                sb.append("Disposed at warehouse ");
                if (tx.getToWarehouse() != null)
                    sb.append(tx.getToWarehouse().getWarehouseName());
                break;
            default:
                sb.append("Other transaction");
        }
        return sb.toString();
    }
}
