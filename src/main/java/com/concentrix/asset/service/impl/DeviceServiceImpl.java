package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.UpdateDeviceRequest;
import com.concentrix.asset.dto.request.UpdateSeatNumberRequest;
import com.concentrix.asset.dto.response.DeviceBorrowingInfoResponse;
import com.concentrix.asset.dto.response.DeviceMovementHistoryResponse;
import com.concentrix.asset.dto.response.DeviceResponse;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    DeviceUserRepository deviceUserRepository;
    DeviceWarehouseRepository deviceWarehouseRepository;
    DeviceFloorRepository deviceFloorRepository;
    BigfixDataRepository bigfixDataRepository;
    CortexDataRepository cortexDataRepository;
    CrowdstrikeDataRepository crowdstrikeDataRepository;
    SnapshotDeviceRepository snapshotDeviceRepository;

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
        return deviceMapper.toDeviceResponse(device);

    }

    @Override
    public Page<DeviceResponse> filterDevices(String search, DeviceType type, Integer modelId, DeviceStatus status,
            Pageable pageable) {
        return deviceRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                List<Predicate> searchPredicates = new ArrayList<>();

                // Tìm kiếm theo deviceName và serialNumber
                searchPredicates.add(cb.like(cb.lower(root.get("deviceName")), like));
                searchPredicates.add(cb.like(cb.lower(root.get("serialNumber")), like));

                // Sử dụng LEFT JOIN để xử lý relationship có thể null
                var floorJoin = root.join("currentFloor", jakarta.persistence.criteria.JoinType.LEFT);
                var warehouseJoin = root.join("currentWarehouse", jakarta.persistence.criteria.JoinType.LEFT);

                // Tìm kiếm theo floorName với LEFT JOIN
                searchPredicates.add(cb.like(cb.lower(floorJoin.get("floorName")), like));

                // Tìm kiếm theo warehouseName với LEFT JOIN
                searchPredicates.add(cb.like(cb.lower(warehouseJoin.get("warehouseName")), like));

                predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
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
        // final LocalDate purchaseDate;
        PODetail poDetail = poDetailRepository.findByDevice_DeviceIdAndDevice_SerialNumberNotNull(device.getDeviceId());
        // if (poDetail != null && poDetail.getPurchaseOrder() != null) {
        // purchaseDate = poDetail.getPurchaseOrder().getCreatedAt();
        // } else {
        // purchaseDate = null;
        // }
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
                .map(TransactionDetail::getTransaction)
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
    public List<DeviceBorrowingInfoResponse.DeviceInfo> getBorrowingDevicesByUser(String eid) {

        List<DeviceBorrowingInfoResponse.DeviceInfo> result = new ArrayList<>();

        List<Device> devicesWithSerial = deviceRepository.findAllByCurrentUser_Eid(eid);

        for (Device dv : devicesWithSerial) {
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

        List<DeviceUser> devicesWithoutSerial = deviceUserRepository.findAllByUser_Eid(eid);
        for (DeviceUser du : devicesWithoutSerial) {
            Device dv = du.getDevice();
            Integer quantity = du.getQuantity();

            if (quantity != null && quantity > 0) {
                DeviceBorrowingInfoResponse.DeviceInfo deviceInfo = DeviceBorrowingInfoResponse.DeviceInfo.builder()
                        .serialNumber(dv.getSerialNumber())
                        .deviceName(dv.getDeviceName())
                        .modelId(
                                dv.getModel() != null
                                        ? dv.getModel().getModelId() == null ? null
                                                : dv.getModel().getModelId()
                                        : null)
                        .modelName(dv.getModel() != null ? dv.getModel().getModelName() : null)
                        .quantity(quantity)
                        .build();
                result.add(deviceInfo);
            }

        }

        return result;
    }

    @Override
    public Page<DeviceBorrowingInfoResponse> getUsersBorrowingDevice(Pageable pageable) {
        return deviceRepository.findUsersWithDevices(pageable)
                .map(user -> DeviceBorrowingInfoResponse.builder()
                        .eid(user.getEid())
                        .fullName(user.getFullName())
                        .build());
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
        if (device.getModel().getType() == DeviceType.LAPTOP) {
            int lenSN = device.getSerialNumber().length();
            String serialNumber = device.getModel().getManufacturer().equalsIgnoreCase("dell")
                    ? device.getSerialNumber().substring(0, 6)
                    : device.getSerialNumber().substring(lenSN - 6, lenSN);
            return "VNHCM-LAP" +
                    serialNumber;
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

        if (floor.getAccount() == null || floor.getAccount().getAccountCode() == null) {
            return null;
        }

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

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteDevice(Integer deviceId) {

        // Kiểm tra device có tồn tại không
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND, deviceId));

        // Xóa các bản ghi liên quan theo thứ tự
        // 1. Xóa BigfixData
        bigfixDataRepository.findByDevice(device).ifPresent(bigfixData -> {
            log.info("Deleting BigfixData for device ID: {}", deviceId);
            bigfixDataRepository.delete(bigfixData);
        });

        // 2. Xóa CortexData
        cortexDataRepository.findByDevice(device).ifPresent(cortexData -> {
            log.info("Deleting CortexData for device ID: {}", deviceId);
            cortexDataRepository.delete(cortexData);
        });

        // 3. Xóa CrowdstrikeData
        crowdstrikeDataRepository.findByDevice(device).ifPresent(crowdstrikeData -> {
            log.info("Deleting CrowdstrikeData for device ID: {}", deviceId);
            crowdstrikeDataRepository.delete(crowdstrikeData);
        });

        // 4. Xóa DeviceUser records
        List<DeviceUser> deviceUsers = device.getDeviceUsers();
        if (deviceUsers != null && !deviceUsers.isEmpty()) {
            log.info("Deleting {} DeviceUser records for device ID: {}", deviceUsers.size(), deviceId);
            deviceUserRepository.deleteAll(deviceUsers);
        }

        // 5. Xóa DeviceWarehouse records
        List<DeviceWarehouse> deviceWarehouses = device.getDeviceWarehouses();
        if (deviceWarehouses != null && !deviceWarehouses.isEmpty()) {
            log.info("Deleting {} DeviceWarehouse records for device ID: {}", deviceWarehouses.size(), deviceId);
            deviceWarehouseRepository.deleteAll(deviceWarehouses);
        }

        // 6. Xóa DeviceFloor records
        List<DeviceFloor> deviceFloors = device.getDeviceFloors();
        if (deviceFloors != null && !deviceFloors.isEmpty()) {
            log.info("Deleting {} DeviceFloor records for device ID: {}", deviceFloors.size(), deviceId);
            deviceFloorRepository.deleteAll(deviceFloors);
        }

        // 7. Xóa TransactionDetail records
        List<TransactionDetail> transactionDetails = transactionDetailRepository
                .findAllByDevice_DeviceIdOrderByTransaction_TransactionIdAsc(deviceId);
        if (!transactionDetails.isEmpty()) {
            log.info("Deleting {} TransactionDetail records for device ID: {}", transactionDetails.size(), deviceId);
            transactionDetailRepository.deleteAll(transactionDetails);
        }

        // 8. Xóa PODetail records
        List<PODetail> poDetails = device.getPoDetails();
        if (poDetails != null && !poDetails.isEmpty()) {
            log.info("Deleting {} PODetail records for device ID: {}", poDetails.size(), deviceId);
            poDetailRepository.deleteAll(poDetails);
        }

        // 9. Xóa SnapshotDevice records
        List<SnapshotDevice> snapshotDevices = snapshotDeviceRepository.findAllByDevice(device);
        if (!snapshotDevices.isEmpty()) {
            log.info("Deleting {} SnapshotDevice records for device ID: {}", snapshotDevices.size(), deviceId);
            snapshotDeviceRepository.deleteAll(snapshotDevices);
        }

        // 10. Cuối cùng, xóa Device
        log.info("Deleting device with ID: {}", deviceId);
        deviceRepository.delete(device);
        log.info("Successfully deleted device with ID: {}", deviceId);
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
