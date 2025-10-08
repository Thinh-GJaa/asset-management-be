package com.concentrix.asset.scheduling;


import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.enums.Role;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.service.EmailService;
import com.concentrix.asset.service.RemindService;
import jakarta.mail.MessagingException;
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
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // bỏ makeFinal
public class ReminderUploadHandoverImageJob {

    EmailService emailService;
    RemindService remindService;
    UserRepository userRepository;

    @NonFinal
    @Value("${app.notification.owner-email}")
    String ownerEmail;

    @NonFinal
    @Value("${app.notification.system-alert-email}")
    String alertSystemEmail;

    @Scheduled(cron = "0 45 17 ? * MON-SAT")    // 17:45 từ T2 → T7
    public void sendReminderUploadHandoverImage() throws MessagingException{
        try{
            Map<Site, List<AssetTransaction>> transactionMap = remindService.calculateHandoverImageReminder();

            transactionMap.forEach((
                    site, transactions) -> {
                String html = buildHtmlReminder(site, transactions);

                String emailLeader = userRepository.findEmailByRoleAndSiteId(Role.LEADER, site.getSiteId())
                        .stream().findFirst().orElse(null);
                List<String> ccList = userRepository.findEmailByRoleAndSiteId(Role.IT, site.getSiteId());

                String subject = "[AMS_VN] Reminder Upload Handover Image";

                try {
                    emailService.sendEmail(emailLeader, subject, html, ccList, List.of(alertSystemEmail));
                } catch (MessagingException e) {
                    throw new RuntimeException(e);
                }
            });

        }catch(Exception e){
            log.error("[SCHEDULER] Failed to send reminder upload handover image", e);
            String subject = "Reminder Upload Handover Image Error";
            String content = e.getMessage();
            emailService.sendEmail(alertSystemEmail, subject, content, null, null);
            throw new RuntimeException(e);
        }

    }


    public String buildHtmlReminder(Site site, List<AssetTransaction> transactions) {
        StringBuilder html = new StringBuilder();

        html.append("<html><body style='font-family: Arial, sans-serif; background-color:#f9f9f9; padding:20px;'>");

        // Container
        html.append("<div style='max-width:800px; margin:auto; background:#ffffff; padding:20px; border-radius:8px; box-shadow:0 2px 8px rgba(0,0,0,0.1);'>");

        // Title
        html.append("<h2 style='color:#2E86C1; text-align:center; margin-bottom:20px;'>")
                .append("📌 Reminder: Missing Images for Site <span style='color:#000;'>")
                .append(site.getSiteName())
                .append("</span></h2>");

        // Transactions table
        html.append("<p style='font-size:14px; color:#333;'>The following transactions do not have images attached:</p>");
        html.append("<table style='border-collapse:collapse; width:100%; font-size:14px;'>");
        html.append("<thead style='background-color:#2E86C1; color:#fff;'>")
                .append("<tr>")
                .append("<th style='padding:8px; border:1px solid #ddd;'>ID</th>")
                .append("<th style='padding:8px; border:1px solid #ddd;'>Type</th>")
                .append("<th style='padding:8px; border:1px solid #ddd;'>Created By</th>")
                .append("<th style='padding:8px; border:1px solid #ddd;'>Created At</th>")
                .append("<th style='padding:8px; border:1px solid #ddd;'>User</th>")
                .append("<th style='padding:8px; border:1px solid #ddd;'>SSO</th>")
                .append("</tr>")
                .append("</thead><tbody>");

        for (AssetTransaction tx : transactions) {
            html.append("<tr style='background-color:")
                    .append((tx.getTransactionId() % 2 == 0) ? "#fdfdfd" : "#f7f9fc")
                    .append(";'>")
                    .append("<td style='padding:8px; border:1px solid #ddd; text-align:center;'>").append(tx.getTransactionId()).append("</td>")
                    .append("<td style='padding:8px; border:1px solid #ddd;'>").append(tx.getTransactionType()).append("</td>")
                    .append("<td style='padding:8px; border:1px solid #ddd;'>").append(tx.getCreatedBy().getFullName()).append("</td>")
                    .append("<td style='padding:8px; border:1px solid #ddd;'>").append(tx.getCreatedAt()).append("</td>")
                    .append("<td style='padding:8px; border:1px solid #ddd;'>").append(tx.getUserUse() != null ? tx.getUserUse().getFullName() : "N/A").append("</td>")
                    .append("<td style='padding:8px; border:1px solid #ddd; text-align:center;'>").append(tx.getUserUse() != null ? tx.getUserUse().getSso() : "N/A").append("</td>")
                    .append("</tr>");
        }

        html.append("</tbody></table>");

        // Footer
        html.append("<p style='margin-top:20px; font-size:13px; color:#555;'>")
                .append("⚠️ Please update the missing images as soon as possible to complete the process.<br>")
                .append("Thank you for your cooperation.")
                .append("</p>");

        html.append("</div></body></html>");

        return html.toString();
    }






}
