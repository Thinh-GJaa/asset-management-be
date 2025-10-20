package com.concentrix.asset.scheduling;

import com.concentrix.asset.dto.response.LowStockResponse;
import com.concentrix.asset.enums.Role;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.service.EmailService;
import com.concentrix.asset.service.LowStockService;
import jakarta.mail.MessagingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // bỏ makeFinal
public class LowStockNotificationJob {

    EmailService emailService;
    UserRepository userRepository;
    LowStockService lowStockService;

    @NonFinal
    @Value("${app.notification.owner-email}")
    String ownerEmail;

    @NonFinal
    @Value("${app.notification.system-alert-email}")
    String alertSystemEmail;

    // Scheduled: chạy 10h sáng mỗi thứ 2
    @Scheduled(cron = "0 0 10 * * 2")
    public void sendLowStockNotifications() throws MessagingException {
        String subject = "[AMS_VN]Low Stock Devices Report";
        String body = buildLowStockHtmlTable(lowStockService.getLowStockDevices());
        try {
            List<String> ccList = userRepository.findEmailByRoleAndSiteId(Role.LEADER, null);

            ccList.add("diemmi.nguyenthi@concentrix.com.vn"); // Add thêm email của chị Mi vào notification low stock

            emailService.sendEmail(ownerEmail, subject, body, ccList, List.of(alertSystemEmail));
            log.info("[SCHEDULER] Email sent to {} and cc {}", ownerEmail, ccList);
        } catch (Exception e) {
            log.error("[SCHEDULER] Failed to send email to {}: {}", ownerEmail, e.getMessage());

            String subjectError = "[AMS_VN]Low Stock Devices Report Error";
            String bodyError = "Failed to send email low stock devices report";
            emailService.sendEmail(alertSystemEmail, subjectError, bodyError, null, null);
        }
    }

    public String buildLowStockHtmlTable(List<LowStockResponse> lowStockList) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset='utf-8'><style>")
                .append("body{font-family:Arial,Helvetica,sans-serif;color:#333}")
                .append("h2{color:#2c3e50;margin-bottom:4px}")
                .append(".subtitle{color:#555;font-size:13px;margin-top:0;margin-bottom:12px}")
                .append("table{border-collapse:collapse;width:100%}")
                .append("th,td{border:1px solid #e6e6e6;padding:10px;text-align:center}")
                .append("th{background:#2f4b5a;color:#fff;font-weight:600}")
                .append("tr:nth-child(even){background:#fbfbfb}")
                .append(".deviceCell{text-align:left;padding-left:12px;font-weight:600}")
                // thêm class highlight
                .append(".highlightCell{background:#eaf6ff;font-weight:bold;color:#2c3e50}")
                .append("</style></head><body><div class='container'>")
                .append("<h2>Low Stock Devices Report</h2>");

        if (lowStockList == null || lowStockList.isEmpty()) {
            html.append("<p>Not found data.</p></div></body></html>");
            return html.toString();
        }

        // canonical key -> display name
        Map<String, String> deviceDisplay = new LinkedHashMap<>();
        Map<String, Integer> totalMap = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> availableMap = new LinkedHashMap<>();
        LinkedHashSet<String> siteNames = new LinkedHashSet<>();

        for (LowStockResponse site : lowStockList) {
            String siteName = site.getSiteName().trim();
            siteNames.add(siteName);

            for (LowStockResponse.LowStockType t : site.getLowStockTypes()) {
                String display = t.getType().toString();
                String key = display.toUpperCase();

                deviceDisplay.putIfAbsent(key, display);
                totalMap.put(key, t.getTotal());

                Map<String, Integer> bySite = availableMap.computeIfAbsent(key, k -> new LinkedHashMap<>());
                bySite.put(siteName, t.getAvailable());
            }
        }

        // Header
        html.append("<table><thead><tr>")
                .append("<th>Device Type (Total)</th>");
        for (String s : siteNames) {
            html.append("<th>").append(escapeHtml(s)).append("</th>");
        }
        html.append("</tr></thead><tbody>");

        // Rows
        for (String key : deviceDisplay.keySet()) {
            int total = totalMap.getOrDefault(key, 0);
            String display = deviceDisplay.get(key);

            html.append("<tr>")
                    .append("<td class='deviceCell'>").append(escapeHtml(display))
                    .append(" (").append(total).append(")</td>");

            for (String site : siteNames) {
                String cellValue = "> 4-5%"; // mặc định là không có
                Map<String, Integer> bySite = availableMap.get(key);
                boolean hasData = false;
                if (bySite != null && bySite.containsKey(site)) {
                    cellValue = String.valueOf(bySite.get(site)); // có dữ liệu, kể cả = 0
                    hasData = true;
                }

                if (hasData) {
                    html.append("<td class='highlightCell'>").append(cellValue).append("</td>");
                } else {
                    html.append("<td>").append(cellValue).append("</td>");
                }
            }

            html.append("</tr>");
        }

        html.append("</tbody></table></div></body></html>");
        return html.toString();
    }

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
