package com.concentrix.asset.service.impl.transaction;

import com.concentrix.asset.dto.request.CreateAssignmentRequest;
import com.concentrix.asset.dto.request.LaptopBadgeRequest;
import com.concentrix.asset.dto.response.AssetHandoverResponse;
import com.concentrix.asset.dto.response.AssignmentResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.AssignmentMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.EmailService;
import com.concentrix.asset.service.UserService;
import com.concentrix.asset.service.transaction.AssignmentService;
import jakarta.mail.MessagingException;
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
import java.util.Arrays;
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
    EmailService emailService;

    @NonFinal
    @Value("${app.path.upload.handover}")
    String handoverImageFolder;


    @NonFinal
    @Value("${app.notification.security-email}")
    String securityEmailsString;

    @NonFinal
    @Value("${app.notification.local-it-email}")
    String localITEmail;

    @NonFinal
    @Value("${app.notification.system-alert-email}")
    String alertSystemEmail;

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
                        assignmentId,
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
                log.error("Error while saving file for transaction {}", assignmentId, e);
            }
        }
    }

    @Override
    public void requestLaptopBadge(LaptopBadgeRequest request) throws MessagingException {

        //Kiểm tra transactionId có tồn tại hay không
        AssetTransaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, request.getTransactionId()));

        //Kiểm tra transaction type có hợp lệ hay không
        if(!transaction.getTransactionType().equals(TransactionType.ASSIGNMENT))
            throw new CustomException(ErrorCode.TRANSACTION_TYPE_INVALID);

        // Lấy danh sách serial hiện có trong transaction details
        List<String> transactionSerials = transaction.getDetails().stream()
                .map(detail -> detail.getDevice().getSerialNumber())
                .toList();

        List<Device> devices = new ArrayList<>();

        // Kiểm tra từng serial trong listSerial
        for (String serial : request.getListSerial()) {
            Device device = deviceRepository.findBySerialNumber(serial)
                    .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND, serial));

            // Nếu serial không nằm trong transaction details → báo lỗi
            if (!transactionSerials.contains(serial)) {
                throw new CustomException(ErrorCode.SERIAL_NOT_IN_TRANSACTION, serial, request.getTransactionId());
            }

            devices.add(device);
        }

        //Email user and email Local IT team
        List<String> ccList = Arrays.asList(transaction.getUserUse().getEmail(), localITEmail);

        String subject = "Laptop Badge request for "+ transaction.getUserUse().getEmail();

        String html = buildLaptopBadgeHtmlTemplate(transaction, devices);

        emailService.sendEmail(securityEmailsString, subject, html, ccList, List.of(alertSystemEmail));

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

    public String buildLaptopBadgeHtmlTemplate(AssetTransaction transaction, List<Device> devices) {
        StringBuilder html = new StringBuilder();

        // --- HEADER + STYLE ---
        html.append("<html><head><meta charset='UTF-8'><style>")
                .append("body{font-family:Arial,Helvetica,sans-serif;color:#000;font-size:14px;}")
                .append("table{border-collapse:collapse;width:100%;margin-top:10px;}")
                .append("th,td{border:1px solid #0b0b0b;padding:8px 10px;text-align:left;}")
                .append("th{background-color:#ffeb3b;font-weight:bold;}")
                .append("td{background-color:#10334b;color:#fff;}")
                .append("a{color:#64b5f6;text-decoration:none;}")
                .append(".container{margin:10px 0;}")
                .append("</style></head><body>")
                .append("<div class='container'>")
                .append("<p>Hi Security team,</p>")
                .append("<p>Please help provide laptop badge as the following information.</p>");

        // --- KIỂM TRA DỮ LIỆU ---
        if (transaction == null || devices == null || devices.isEmpty()) {
            html.append("<p>No laptop badge request data found.</p>")
                    .append("</div></body></html>");
            return html.toString();
        }

        // --- BẢNG HEADER ---
        html.append("<table><thead><tr>")
                .append("<th>Employee ID</th>")
                .append("<th>Name</th>")
                .append("<th>Role</th>")
                .append("<th>Serial Number</th>")
                .append("<th>Laptop Badge</th>")
                .append("<th>User Email</th>")
                .append("<th>Model</th>")
                .append("<th>Location</th>")
                .append("</tr></thead><tbody>");

        // --- GHI DỮ LIỆU ---
        String employeeId = String.valueOf(transaction.getUserUse().getEid());
        String fullName = transaction.getUserUse().getFullName();
        String role = transaction.getUserUse().getJobTitle();
        String email = transaction.getUserUse().getEmail();
        String location = transaction.getFromWarehouse().getSite().getSiteName();

        for (Device device : devices) {
            html.append("<tr>")
                    .append("<td>").append(escapeHtml(employeeId)).append("</td>")
                    .append("<td>").append(escapeHtml(fullName)).append("</td>")
                    .append("<td>").append(escapeHtml(role)).append("</td>")
                    .append("<td>").append(escapeHtml(device.getSerialNumber())).append("</td>")
                    .append("<td>").append("White").append("</td>") // ✅ luôn White
                    .append("<td>").append(escapeHtml(email)).append("</td>")
                    .append("<td>").append(escapeHtml(device.getModel() != null ? device.getModel().getModelName() : "---")).append("</td>")
                    .append("<td>").append(escapeHtml(location)).append("</td>")
                    .append("</tr>");
        }

        html.append("</tbody></table>");

        // --- LỜI CHÀO CÁ NHÂN ---
        html.append("<p><br>Hi ").append(escapeHtml(fullName)).append(",</p>")
                .append("<p>To create laptop badge, please provide a photo of yourself ")
                .append("to the security team for further processing.</p>");


        html.append("</div></body></html>");

        return html.toString();
    }

    /** Escape HTML để tránh lỗi khi có ký tự đặc biệt */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }



}
