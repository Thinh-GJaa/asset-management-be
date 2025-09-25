package com.concentrix.asset.service.impl.transaction;

import com.concentrix.asset.dto.request.CreateTransferRequest;
import com.concentrix.asset.dto.response.TransferResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.Role;
import com.concentrix.asset.enums.TransactionStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.TransferMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.EmailService;
import com.concentrix.asset.service.UserService;
import com.concentrix.asset.service.transaction.TransferService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class TransferServiceImpl implements TransferService {

    TransactionRepository transactionRepository;
    TransferMapper transferMapper;
    DeviceRepository deviceRepository;
    DeviceWarehouseRepository deviceWarehouseRepository;
    WarehouseRepository warehouseRepository;
    UserService userService;
    EmailService emailService;
    UserRepository userRepository;

    @Value("${app.notification.owner-email}")
    @NonFinal
    String ownerEmail;

    @Override
    public TransferResponse getTransferById(Integer transferId) {

        AssetTransaction transaction = transactionRepository.findById(transferId).orElseThrow(
                () -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, transferId));

        return transferMapper.toTransferResponse(transaction);
    }

    @Override
    public TransferResponse createTransfer(CreateTransferRequest request) {
        AssetTransaction transaction = transferMapper.toAssetTransaction(request);
        transaction.setCreatedBy(userService.getCurrentUser());
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
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Nếu có serialNumber không tìm thấy thì trả về list
        if (!serialNotFound.isEmpty()) {
            throw new CustomException(ErrorCode.DEVICE_NOT_FOUND, String.join(",", serialNotFound));
        }

        transaction.setDetails(details);

        transaction = transactionRepository.save(transaction);
        updateDeviceAndWarehousesForTransfer(transaction);

        // Send email notification for new transfer
        sendTransferCreatedEmail(transaction);

        return transferMapper.toTransferResponse(transaction);
    }

    @Override
    public void approveTransfer(Integer transactionId) {
        AssetTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, transactionId));

        if (transaction.getTransactionType() != TransactionType.TRANSFER_SITE) {
            throw new CustomException(ErrorCode.TRANSACTION_TYPE_INVALID, transactionId);
        }

        if (transaction.getTransactionStatus() != TransactionStatus.PENDING) {
            throw new CustomException(ErrorCode.TRANSACTION_STATUS_INVALID, transactionId);
        }

        User currentUser = userService.getCurrentUser();
        if(!Role.ADMIN.equals(currentUser.getRole()))
            throw new CustomException(ErrorCode.UNAUTHORIZED);

        transaction.setTransactionStatus(TransactionStatus.APPROVED);
        transactionRepository.save(transaction);

        // Send email notification for approved transfer
        sendTransferApprovedEmail(transaction);
    }

    @Override
    public void approveTransferByToken(String token) {
        log.info("[TransferServiceImpl] Approving transfer by token: {}", token);
        approveTransfer(Integer.parseInt(token));
    }

    public void confirmTransfer(Integer transactionId) {
        AssetTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, transactionId));

        if (transaction.getTransactionType() != TransactionType.TRANSFER_SITE) {
            throw new CustomException(ErrorCode.TRANSACTION_TYPE_INVALID, transactionId);
        }

        if (transaction.getTransactionStatus() != TransactionStatus.APPROVED) {
            throw new CustomException(ErrorCode.TRANSACTION_STATUS_INVALID, transactionId);
        }

        User currentUser = userService.getCurrentUser();
        Site currentUserSite = currentUser.getSite();
        if(currentUserSite == null
            || !transaction.getToWarehouse().getSite().getSiteId().equals(currentUserSite.getSiteId()))
                throw new CustomException(ErrorCode.UNAUTHORIZED);

        transaction.setTransactionStatus(TransactionStatus.CONFIRMED);
        transaction.setConfirmedBy(userService.getCurrentUser());

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
    public Page<TransferResponse> filterTransfers(String search, LocalDate fromDate, LocalDate toDate,
            Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }

        return transactionRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("transactionType"), TransactionType.TRANSFER_SITE));
            predicates.add(cb.equal(root.get("transactionStatus"), TransactionStatus.CONFIRMED));

            if (search != null && !search.isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fromWarehouse").get("warehouseName")), searchPattern),
                        cb.like(cb.lower(root.get("toWarehouse").get("warehouseName")), searchPattern),
                        cb.like(cb.lower(root.get("createdBy").get("fullName")), searchPattern)));
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
        }, pageable).map(transferMapper::toTransferResponse);
    }

    @Override
    public Page<TransferResponse> filterTransfersSitePending(Pageable pageable) {

        return transactionRepository.findPendingOrApprovedTransfers(pageable)
                .map(transferMapper::toTransferResponse);
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
                    throw new CustomException(ErrorCode.STOCK_OUT, device.getModel().getModelName(), fromStock.getQuantity());
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

    /**
     * Build HTML content for transfer site notification email
     */
    public String buildTransferNotificationHTML(AssetTransaction transaction, String notificationType) {

        // Build email structure

        return buildEmailHeader(notificationType, transaction) +
                buildTransferInfoSection(transaction, notificationType) +
                buildTransferItemsSection(transaction) +
                buildEmailFooter();
    }

    /**
     * Build email header with CSS styles and title
     */
    private String buildEmailHeader(String notificationType, AssetTransaction transaction) {
        String title = getNotificationTitle(notificationType);

        return "<html><head><meta charset='utf-8'><style>"
                + "body{font-family:Arial,Helvetica,sans-serif;color:#333;margin:0;padding:20px;background-color:#f8f9fa}"
                + ".container{max-width:800px;margin:0 auto;background:white;border-radius:8px;box-shadow:0 2px 10px rgba(0,0,0,0.1);overflow:hidden}"
                + ".header{background:linear-gradient(135deg,#2c3e50,#34495e);color:white;padding:20px;text-align:center}"
                + ".header h1{margin:0;font-size:24px;font-weight:600}"
                + ".header p{margin:10px 0 0 0;font-size:14px;opacity:0.9}"
                + ".action-button{display:inline-block;margin:15px 0;padding:12px 24px;background:#3498db;color:white;text-decoration:none;border-radius:6px;font-weight:600;font-size:14px;transition:background 0.3s}"
                + ".action-button:hover{background:#2980b9}"
                + ".action-button.approve{background:#27ae60}"
                + ".action-button.approve:hover{background:#229954}"
                + ".action-button.view{background:#9b59b6}"
                + ".action-button.view:hover{background:#8e44ad}"
                + ".content{padding:30px}"
                + ".info-section{margin-bottom:25px}"
                + ".info-section h3{color:#2c3e50;margin-bottom:15px;font-size:18px;border-bottom:2px solid #ecf0f1;padding-bottom:8px}"
                + ".info-grid{display:grid;grid-template-columns:1fr 1fr;gap:15px;margin-bottom:20px}"
                + ".info-item{background:#f8f9fa;padding:12px;border-radius:6px;border-left:4px solid #3498db}"
                + ".info-label{font-weight:600;color:#2c3e50;font-size:14px}"
                + ".info-value{color:#555;margin-top:4px}"
                + ".status-badge{display:inline-block;padding:6px 12px;border-radius:20px;font-weight:600;font-size:12px;text-transform:uppercase}"
                + ".items-table{width:100%;border-collapse:collapse;margin-top:15px}"
                + ".items-table th{background:#34495e;color:white;padding:12px;text-align:left;font-weight:600}"
                + ".items-table td{padding:12px;border-bottom:1px solid #ecf0f1}"
                + ".items-table tr:nth-child(even){background:#f8f9fa}"
                + ".footer{background:#ecf0f1;padding:20px;text-align:center;color:#7f8c8d;font-size:14px}"
                + ".highlight{background:#e8f4fd;padding:2px 6px;border-radius:4px;font-weight:600}"
                + "</style></head><body>"
                + "<div class='container'>"
                + "<div class='header'>"
                + "<h1>" + escapeHtml(title) + "</h1>"
                + "</div>"
                + "<div class='content'>";
    }

    /**
     * Get notification title based on type
     */
    private String getNotificationTitle(String notificationType) {
        if ("CREATED".equals(notificationType)) {
            return "🔄 New Site Transfer Request Created";
        } else if ("APPROVED".equals(notificationType)) {
            return "✅ Site Transfer Request Approved";
        }
        return "📋 Site Transfer Notification";
    }

//    /**
//     * Build action button based on notification type
//     */
//    private String buildActionButton(String notificationType, AssetTransaction transaction) {
//        String buttonHtml = "";
//
//        if ("CREATED".equals(notificationType)) {
//            // For created transfer, show approve button
//            buttonHtml = "<a href='" + url_base + "' class='action-button approve'>👁️ View</a>";
//        } else if ("APPROVED".equals(notificationType)) {
//            // For approved transfer, show view detail button
//            buttonHtml = "<a href='" + url_base + "' class='action-button view'>👁️ View</a>";
//        }
//
//        return buttonHtml;
//    }
//
//    /**
//     * Build call-to-action section placed above the footer
//     */
//    private String buildActionSection(String notificationType, AssetTransaction transaction) {
//        String button = buildActionButton(notificationType, transaction);
//        if (button == null || button.isEmpty()) {
//            return "";
//        }
//        return "<div class='info-section' style='text-align:center;margin-top:8px'>" +
//                button +
//                "</div>";
//    }

    /**
     * Build transfer information section
     */
    private String buildTransferInfoSection(AssetTransaction transaction, String notificationType) {
        StringBuilder section = new StringBuilder();

        section.append("<div class='info-section'>")
                .append("<h3>📋 Transfer Information</h3>")
                .append("<div class='info-grid'>");

        // Transfer ID
        section.append(buildInfoItem("Transfer ID", "#" + transaction.getTransactionId()));

        // From Site
        String fromSite = transaction.getFromWarehouse().getSite().getSiteName() +
                " - " + transaction.getFromWarehouse().getWarehouseName();
        section.append(buildInfoItem("From Site", fromSite, true, false));

        // To Site
        String toSite = transaction.getToWarehouse().getSite().getSiteName() +
                " - " + transaction.getToWarehouse().getWarehouseName();
        section.append(buildInfoItem("To Site", toSite, true, false));

        // Created By
        section.append(buildInfoItem("Created By", transaction.getCreatedBy().getFullName()));

        // Created Date
        section.append(buildInfoItem("Created Date", transaction.getCreatedAt().toString()));

        section.append("</div>");

        // Add note if exists
        if (transaction.getNote() != null && !transaction.getNote().trim().isEmpty()) {
            section.append(buildInfoItem("Note", transaction.getNote(), false, true));
        }

        section.append("</div>");

        return section.toString();
    }

    /**
     * Build info item HTML
     */
    private String buildInfoItem(String label, String value) {
        return buildInfoItem(label, value, false, false);
    }

    /**
     * Build info item HTML with options
     */
    private String buildInfoItem(String label, String value, boolean highlight, boolean fullWidth) {
        String style = fullWidth ? " style='grid-column:1/-1'" : "";
        String valueHtml = highlight ? "<span class='highlight'>" + escapeHtml(value) + "</span>" : escapeHtml(value);

        return "<div class='info-item'" + style + ">"
                + "<div class='info-label'>" + escapeHtml(label) + "</div>"
                + "<div class='info-value'>" + valueHtml + "</div>"
                + "</div>";
    }

    /**
     * Build transfer items section
     */
    private String buildTransferItemsSection(AssetTransaction transaction) {
        StringBuilder section = new StringBuilder();

        section.append("<div class='info-section'>")
                .append("<h3>📦 Transfer Items</h3>")
                .append("<table class='items-table'>")
                .append("<thead>")
                .append("<tr>")
                .append("<th>Device Name</th>")
                .append("<th>Model</th>")
                .append("<th>Serial Number</th>")
                .append("<th>Quantity</th>")
                .append("</tr>")
                .append("</thead>")
                .append("<tbody>");

        // Add items
        if (transaction.getDetails() != null && !transaction.getDetails().isEmpty()) {
            for (TransactionDetail detail : transaction.getDetails()) {
                section.append(buildItemRow(detail));
            }
        } else {
            section.append("<tr><td colspan='4' style='text-align:center;color:#7f8c8d'>No items found</td></tr>");
        }

        section.append("</tbody>")
                .append("</table>")
                .append("</div>");

        return section.toString();
    }

    /**
     * Build item row HTML
     */
    private String buildItemRow(TransactionDetail detail) {
        Device device = detail.getDevice();
        String serialNumber = device.getSerialNumber() != null ? device.getSerialNumber() : "N/A";

        return "<tr>"
                + "<td>" + escapeHtml(device.getDeviceName()) + "</td>"
                + "<td>" + escapeHtml(device.getModel().getModelName()) + "</td>"
                + "<td>" + escapeHtml(serialNumber) + "</td>"
                + "<td>" + detail.getQuantity() + "</td>"
                + "</tr>";
    }

    /**
     * Build email footer
     */
    private String buildEmailFooter() {
        return "<div class='footer'>"
                + "<p>This is an automated notification from Asset Management System (AMS)</p>"
                + "<p>Please do not reply to this email</p>"
                + "</div>"
                + "</div>"
                + "</body></html>";
    }

    /**
     * Send email notification when transfer is created
     */
    private void sendTransferCreatedEmail(AssetTransaction transaction) {
        try {
            String subject = "[AMS_VN] New Site Transfer Request - #" + transaction.getTransactionId();
            String htmlBody = buildTransferNotificationHTML(transaction, "CREATED");

            emailService.sendEmail(ownerEmail, subject, htmlBody, null);

            log.info("[TransferServiceImpl] Transfer created email sent for transfer #{}",
                    transaction.getTransactionId());
        } catch (Exception e) {
            log.error("[TransferServiceImpl] Failed to send transfer created email for transfer #{}: {}",
                    transaction.getTransactionId(), e.getMessage(), e);
        }
    }

    /**
     * Send email notification when transfer is approved
     */
    private void sendTransferApprovedEmail(AssetTransaction transaction) {
        try {
            String subject = "[AMS_VN] Site Transfer Request Approved - #" + transaction.getTransactionId();
            String htmlBody = buildTransferNotificationHTML(transaction, "APPROVED");


            List<String> ccList = userRepository.findEmailByRoleAndSiteId(
                    Role.IT, transaction.getToWarehouse().getSite().getSiteId());

            List<String> emails = userRepository.findEmailByRoleAndSiteId(
                    Role.LEADER, transaction.getToWarehouse().getSite().getSiteId());

            String toEmail = emails.stream().findFirst().orElse(null);

            emailService.sendEmail(ownerEmail, subject, htmlBody, ccList);
            log.info("[TransferServiceImpl] Transfer approved email sent for transfer #{}",
                    transaction.getTransactionId());

        } catch (Exception e) {
            log.error("[TransferServiceImpl] Failed to send transfer approved email for transfer #{}: {}",
                    transaction.getTransactionId(), e.getMessage(), e);
        }
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

}
