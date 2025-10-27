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
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    DeviceUserRepository deviceUserRepository;
    TransactionImageRepository transactionImageRepository;

    @NonFinal
    @Value("${app.path.upload.handover}")
    String handoverImageFolder;

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
                        // Serial: chỉ cho phép trả nếu đang ASSIGNED hoặc WAH cho đúng user
                        if (device.getStatus() != DeviceStatus.ASSIGNED && device.getStatus() != DeviceStatus.WAH) {
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
                .filter(Objects::nonNull)
                .toList();

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
    public Page<ReturnFromUserResponse> filterReturnFromUsers(String search, LocalDate fromDate, LocalDate toDate,
            Pageable pageable) {
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
                // Non-serial
                // Cộng số lượng vào kho đích
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

                // Cập nhật số lượng mượn của user
                DeviceUser deviceUser = deviceUserRepository
                        .findByDevice_DeviceIdAndUser_Eid(deviceId, transaction.getUserUse().getEid())
                        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_DEVICE_USER,
                                device.getModel().getModelName()));

                if (deviceUser.getQuantity() < qty) {
                    throw new CustomException(ErrorCode.RETURN_QUANTITY_EXCEEDS_BORROWED,
                            device.getModel().getModelName());
                } else {
                    deviceUser.setQuantity(deviceUser.getQuantity() - qty);
                    deviceUserRepository.save(deviceUser);
                }

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

    @Override
    public void uploadImage(Integer returnId, List<MultipartFile> images) {
        AssetTransaction transaction = transactionRepository.findById(returnId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (!transaction.getTransactionType().equals(TransactionType.RETURN_FROM_USER)) {
            throw new CustomException(ErrorCode.TRANSACTION_TYPE_INVALID, returnId);
        }

        if (images == null || images.isEmpty()) {
            throw new CustomException(ErrorCode.IMAGE_NOT_FOUND);
        }

        File dir = new File(handoverImageFolder);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Cannot create upload directory: " + handoverImageFolder);
        }

        String sso = (transaction.getUserUse() != null && transaction.getUserUse().getSso() != null)
                ? transaction.getUserUse().getSso()
                : "unknown";

        String transactionType = transaction.getTransactionType().name();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String dateTime = LocalDateTime.now().format(formatter);

        String serialOrType = "unknown";

        if (transaction.getDetails() != null && !transaction.getDetails().isEmpty()) {
            // 1️⃣ Ưu tiên tìm serialNumber
            for (TransactionDetail detail : transaction.getDetails()) {
                if (detail.getDevice() != null
                        && detail.getDevice().getSerialNumber() != null
                        && !detail.getDevice().getSerialNumber().isBlank()) {
                    serialOrType = detail.getDevice().getSerialNumber();
                    break; // lấy cái đầu tiên có serial rồi thoát luôn
                }
            }

            // 2️⃣ Nếu chưa có serial thì lấy type từ model
            if ("unknown".equals(serialOrType)) {
                TransactionDetail detail = transaction.getDetails().get(0); // fallback device đầu tiên
                if (detail.getDevice() != null
                        && detail.getDevice().getModel() != null
                        && detail.getDevice().getModel().getType() != null) {
                    serialOrType = detail.getDevice().getModel().getType().name();
                }
            }
        }

        for (int i = 0; i < images.size(); i++) {
            MultipartFile file = images.get(i);
            if (file.isEmpty())
                continue;

            try {
                // Lấy extension file gốc (.png, .jpg…)
                String originalFilename = file.getOriginalFilename();
                String ext = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    ext = originalFilename.substring(originalFilename.lastIndexOf("."));
                }

                // 👉 Tạo tên file với serialOrType
                String fileName = String.format("%s_%s_%d_%s_%s_%d%s",
                        sso,
                        dateTime,
                        returnId,
                        transactionType,
                        serialOrType,
                        i + 1,
                        ext);

                Path filePath = Paths.get(handoverImageFolder, fileName);
                Files.write(filePath, file.getBytes());

                TransactionImage transactionImage = TransactionImage.builder()
                        .imageName(fileName)
                        .assetTransaction(transaction)
                        .build();

                transactionImageRepository.save(transactionImage);

                log.info("Saved image: {}", fileName);

            } catch (IOException e) {
                log.error("Error while saving file for transaction {}", returnId, e);
            }
        }
    }
}