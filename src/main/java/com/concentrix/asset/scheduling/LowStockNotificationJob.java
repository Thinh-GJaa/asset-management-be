package com.concentrix.asset.scheduling;


import com.concentrix.asset.dto.response.LowStockResponse;
import com.concentrix.asset.service.EmailService;
import com.concentrix.asset.service.LowStockService;
import com.concentrix.asset.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LowStockNotificationJob {

    EmailService emailService;
    UserService userService;
    LowStockService lowStockService;

//    @Scheduled(cron = "0 0 15 * * ?") // 8:00 AM hàng ngày
    public void sendLowStockNotifications() {

        String to = "xthinh04052002@gmail.com";
        String subject = "Test cron job";
        String body = buildLowStockHtmlTable(lowStockService.getLowStockDevices());
        try {
            emailService.sendEmail(to, subject, body, null);
            log.info("[SCHEDULER] Email sent to {}", to);
        } catch (Exception e) {
            log.error("[SCHEDULER] Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public String buildLowStockHtmlTable(List<LowStockResponse> lowStockList) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h2>Low Stock Devices Report</h2>");

        for (LowStockResponse site : lowStockList) {
            html.append("<h3>Site: ").append(site.getSiteName())
                    .append(" (ID: ").append(site.getSiteId()).append(")</h3>");

            html.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse;'>")
                    .append("<tr style='background-color:#f2f2f2;'>")
                    .append("<th>Device Type</th>")
                    .append("<th>Total</th>")
                    .append("<th>Available</th>")
                    .append("</tr>");

            for (LowStockResponse.LowStockType type : site.getLowStockTypes()) {
                html.append("<tr>")
                        .append("<td>").append(type.getType()).append("</td>")
                        .append("<td>").append(type.getTotal()).append("</td>")
                        .append("<td>").append(type.getAvailable()).append("</td>")
                        .append("</tr>");
            }

            html.append("</table><br/>");
        }

        html.append("</body></html>");
        return html.toString();
    }


}
