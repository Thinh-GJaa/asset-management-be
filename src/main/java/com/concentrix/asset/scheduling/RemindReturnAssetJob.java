package com.concentrix.asset.scheduling;

import com.concentrix.asset.entity.Device;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.service.EmailService;
import com.concentrix.asset.service.ReturnRemindService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RemindReturnAssetJob {

    EmailService emailService;
    ReturnRemindService returnRemindService;

    @NonFinal
    @Value("${app.notification.system-alert-email}")
    String alertSystemEmail;

    // Scheduled: chạy 9h sáng mỗi ngày
    @Scheduled(cron = "0 0 9 * * *")
    public void sendReturnReminders() {
        log.info("[SCHEDULER] Sending return reminders");
        Map<User, Map<Device, Integer>> pendingReturns = returnRemindService.calculatePendingReturnsForAllUsers();
        if (pendingReturns.isEmpty()) {
            log.info("[SCHEDULER] No pending returns found");
        }
        for (Map.Entry<User, Map<Device, Integer>> entry : pendingReturns.entrySet()) {
            User user = entry.getKey();
            Map<Device, Integer> deviceRemainings = entry.getValue();
            String html = buildReturnReminderHtml(user, deviceRemainings);
            try {
                String subject = "[AMS_VN] Device Return Reminder";
                emailService.sendEmail(user.getEmail(), subject, html, null, List.of(alertSystemEmail));
            } catch (Exception e) {
                log.error("[SCHEDULER] Failed to send email to {}: {}", user.getEmail(), e.getMessage());
            }
        }
    }

    /**
     * Build HTML content for device return reminder email for a user
     */
    private String buildReturnReminderHtml(User user, Map<Device, Integer> deviceRemainings) {
        StringBuilder html = new StringBuilder();
        html.append(
                "<div style='font-family:Arial,sans-serif;max-width:800px;margin:40px auto;background:#fff;padding:40px 32px;border-radius:16px;box-shadow:0 4px 24px #e0e7ef;'>");
        html.append("<h2 style='color:#2563eb;text-align:center;font-size:2rem;margin-bottom:16px;'>Dear ")
                .append(user.getFullName()).append("!</h2>");
        html.append(
                "<p style='font-size:18px;text-align:center;margin-bottom:32px;'>Below is the list of devices you need to return today:</p>");
        html.append("<div style='overflow-x:auto;'>");
        html.append(
                "<table style='width:100%;border-collapse:collapse;background:#f8fafc;border-radius:10px;box-shadow:0 2px 8px #e0e7ef;font-size:17px;'>");
        html.append("<thead><tr style='background:#2563eb;color:#fff;'>");
        html.append("<th style='padding:16px 12px;text-align:left;'>Type</th>");
        html.append("<th style='padding:16px 12px;text-align:left;'>Model</th>");
        html.append("<th style='padding:16px 12px;text-align:left;'>Serial</th>");
        html.append("<th style='padding:16px 12px;text-align:center;'>Remaining Quantity</th>");
        html.append("</tr></thead><tbody>");
        for (Map.Entry<Device, Integer> d : deviceRemainings.entrySet()) {
            Device device = d.getKey();
            Integer remain = d.getValue();
            html.append("<tr style='border-bottom:1px solid #e5e7eb;'>")
                    .append("<td style='padding:14px 12px;'>").append(device.getModel().getType()).append("</td>")
                    .append("<td style='padding:14px 12px;'>")
                    .append(device.getModel() != null ? device.getModel().getModelName() : "---").append("</td>")
                    .append("<td style='padding:14px 12px;'>")
                    .append(device.getSerialNumber() != null ? device.getSerialNumber() : "---").append("</td>")
                    .append("<td style='padding:14px 12px;text-align:center;font-weight:bold;color:#ef4444;font-size:18px;'>")
                    .append(remain).append("</td>")
                    .append("</tr>");
        }
        html.append("</tbody></table>");
        html.append("</div>");
        html.append(
                "<p style='margin-top:32px;font-size:16px;color:#334155;text-align:center;'>Please return the devices on time to avoid any impact on your work. Thank you!</p>");
        html.append("</div>");
        return html.toString();
    }

}
