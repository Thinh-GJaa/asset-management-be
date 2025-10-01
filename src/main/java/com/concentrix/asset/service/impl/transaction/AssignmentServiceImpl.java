package com.concentrix.asset.service.impl.transaction;

import com.concentrix.asset.dto.request.CreateAssignmentRequest;
import com.concentrix.asset.dto.response.AssetHandoverResponse;
import com.concentrix.asset.dto.response.AssignmentResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.AssignmentMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.UserService;
import com.concentrix.asset.service.transaction.AssignmentService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AssignmentServiceImpl implements AssignmentService {

    TransactionRepository transactionRepository;
    AssignmentMapper assignmentMapper;
    DeviceRepository deviceRepository;
    DeviceWarehouseRepository deviceWarehouseRepository;
    UserService userService;
    DeviceUserRepository deviceUserRepository;
    TransactionImageRepository transactionImageRepository;

    @NonFinal
    @Value("${app.path.upload.handover}")
    String handoverImageFolder;

    @Override
    public AssignmentResponse getAssignmentById(Integer assignmentId) {

        AssetTransaction transaction = transactionRepository.findById(assignmentId).orElseThrow(
                () -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, assignmentId));

        return assignmentMapper.toAssignmentResponse(transaction);
    }

    @Override
    public AssignmentResponse createAssignment(CreateAssignmentRequest request) {
        AssetTransaction transaction = assignmentMapper.toAssetTransaction(request);
        transaction.setCreatedBy(userService.getCurrentUser());

        if (request.getReturnDate() != null && request.getReturnDate().isBefore(LocalDate.now())) {
            throw new CustomException(ErrorCode.INVALID_RETURN_DATE);
        }

        // Gom các serialNumber không tìm thấy vào một list
        List<String> serialNotFound = new ArrayList<>();
        AssetTransaction finalTransaction = transaction;
        List<TransactionDetail> details = request.getItems().stream()
                .map(item -> {
                    final Device device;
                    if (item.getSerialNumber() != null && !item.getSerialNumber().isEmpty()) {
                        // Tìm device theo serial number - gom lỗi vào list
                        device = deviceRepository.findBySerialNumber(item.getSerialNumber())
                                .orElse(null);
                        if (device == null) {
                            serialNotFound.add(item.getSerialNumber());
                            return null; // sẽ filter sau
                        }
                    } else if (item.getModelId() != null) {
                        // Tìm device theo modelId - báo lỗi từng cái
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
                    detail.setTransaction(finalTransaction); // liên kết ngược
                    return detail;
                })
                .filter(Objects::nonNull)
                .toList();

        // Nếu có serialNumber không tìm thấy thì trả về list
        if (!serialNotFound.isEmpty()) {
            throw new CustomException(ErrorCode.DEVICE_NOT_FOUND, String.join(",", serialNotFound));
        }

        transaction.setDetails(details);

        transaction = transactionRepository.save(transaction);
        updateWarehouses(transaction);

        return assignmentMapper.toAssignmentResponse(transaction);
    }

    @Override
    public Page<AssignmentResponse> filterAssignments(
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {

        return transactionRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Chỉ lấy transaction type = ASSIGNMENT
            predicates.add(cb.equal(root.get("transactionType"), TransactionType.ASSIGNMENT));

            // Search theo nhiều trường
            if (search != null && !search.trim().isEmpty()) {
                String keyword = "%" + search.trim().toLowerCase() + "%";

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("userUse").get("fullName")), keyword),
                        cb.like(cb.lower(root.get("userUse").get("eid")), keyword),
                        cb.like(cb.lower(root.get("createdBy").get("fullName")), keyword),
                        cb.like(cb.lower(root.get("fromWarehouse").get("warehouseName")), keyword)));
            }

            if (fromDate != null) {
                LocalDateTime startOfDay = fromDate.atStartOfDay(); // 00:00:00
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startOfDay));
            }

            if (toDate != null) {
                LocalDateTime endOfDay = toDate.atTime(23, 59, 59); // 23:59:59
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endOfDay));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable).map(assignmentMapper::toAssignmentResponse);
    }

    @Override
    public AssetHandoverResponse getAssetHandoverByAssignmentId(Integer assignmentId) {
        AssetTransaction assignment = transactionRepository.findById(assignmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        // Kiểm tra xem có phải là ASSIGNMENT transaction không
        if (assignment.getTransactionType() != TransactionType.ASSIGNMENT) {
            throw new CustomException(ErrorCode.ASSIGNMENT_NOT_FOUND, assignmentId);
        }

        return assignmentMapper.toAssetHandoverResponse(assignment);

    }

    @Override
    public void uploadImage(Integer assignmentId, List<MultipartFile> images) {
        AssetTransaction transaction = transactionRepository.findById(assignmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        if (!transaction.getTransactionType().equals(TransactionType.ASSIGNMENT)) {
            throw new CustomException(ErrorCode.TRANSACTION_TYPE_INVALID, assignmentId);
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
            if (file.isEmpty()) continue;

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
                        assignmentId,
                        transactionType,
                        serialOrType,
                        i + 1,
                        ext
                );

                Path filePath = Paths.get(handoverImageFolder, fileName);
                Files.write(filePath, file.getBytes());

                TransactionImage transactionImage = TransactionImage.builder()
                        .imageName(fileName)
                        .assetTransaction(transaction)
                        .build();

                transactionImageRepository.save(transactionImage);

                log.info("Saved image: {}", fileName);

            } catch (IOException e) {
                log.error("Error while saving file for transaction {}", assignmentId, e);
            }
        }
    }



    private void updateWarehouses(AssetTransaction transaction) {
        List<String> serialInvalid = new ArrayList<>();

        for (TransactionDetail detail : transaction.getDetails()) {
            Device device = detail.getDevice();
            if (hasSerial(device)) {
                processDeviceWithSerial(device, transaction, serialInvalid);
            } else {
                processDeviceWithoutSerial(device, detail, transaction);
            }
        }

        if (!serialInvalid.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_DEVICE_STATUS, String.join(",", serialInvalid));
        }
    }

    private boolean hasSerial(Device device) {
        return device.getSerialNumber() != null && !device.getSerialNumber().isEmpty();
    }

    private void processDeviceWithSerial(Device device, AssetTransaction transaction, List<String> serialInvalid) {
        boolean invalidStatus = device.getStatus() != DeviceStatus.IN_STOCK;
        boolean invalidWarehouse = device.getCurrentWarehouse() == null
                || !device.getCurrentWarehouse().getWarehouseId()
                .equals(transaction.getFromWarehouse().getWarehouseId());

        if (invalidStatus || invalidWarehouse) {
            serialInvalid.add(device.getSerialNumber());
            return; // Nếu invalid thì không update
        }

        device.setStatus(DeviceStatus.ASSIGNED);
        device.setCurrentUser(transaction.getUserUse());
        device.setCurrentWarehouse(null);
        device.setCurrentFloor(null);
        deviceRepository.save(device);
    }

    private void processDeviceWithoutSerial(Device device, TransactionDetail detail, AssetTransaction transaction) {
        Integer deviceId = device.getDeviceId();
        Integer fromWarehouseId = transaction.getFromWarehouse().getWarehouseId();
        Integer qty = detail.getQuantity();

        DeviceWarehouse fromStock = deviceWarehouseRepository
                .findByWarehouse_WarehouseIdAndDevice_DeviceId(fromWarehouseId, deviceId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE,
                        device.getModel().getModelName(),
                        transaction.getFromWarehouse().getWarehouseName()));

        if (fromStock.getQuantity() < qty) {
            throw new CustomException(ErrorCode.STOCK_OUT, device.getModel().getModelName(), fromStock.getQuantity());
        }

        fromStock.setQuantity(fromStock.getQuantity() - qty);
        deviceWarehouseRepository.save(fromStock);

        // Cập nhật DeviceUser
        DeviceUser deviceUser = deviceUserRepository
                .findByDevice_DeviceIdAndUser_Eid(deviceId, transaction.getUserUse().getEid())
                .orElseGet(() -> DeviceUser.builder()
                        .device(device)
                        .user(transaction.getUserUse())
                        .quantity(0)
                        .build());

        deviceUser.setQuantity(deviceUser.getQuantity() + qty);
        deviceUserRepository.save(deviceUser);
    }

}
