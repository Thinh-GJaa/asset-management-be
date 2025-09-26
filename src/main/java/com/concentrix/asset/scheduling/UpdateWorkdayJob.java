package com.concentrix.asset.scheduling;

import com.concentrix.asset.service.EmailService;
import com.concentrix.asset.service.WorkdayService;
import jakarta.mail.MessagingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateWorkdayJob {

    WorkdayService workdayService;
    EmailService emailService;

    @NonFinal
    @Value("${app.notification.system-alert-email}")
    String alertSystemEmail;

    @NonFinal
    @Value("${app.path.upload.workday}")
    String workdayFolder;

    //Chạy mỗi ngày lúc 22:00 (10 PM)
    @Scheduled(cron = "0 0 22 * * *")
    public void updateWorkday() throws MessagingException {
        try {
            var result = workdayService.importFromWorkday();
            log.info("[Workday][Import] created={}, updated={}", result.get("created"), result.get("updated"));
        } catch (Exception e) {
            log.error("[Workday][Import][ERROR] {}", e.getMessage(), e);
            String subject = "Workday Import Error";
            String content = e.getMessage();
            emailService.sendEmail(alertSystemEmail, subject, content, null);
        }
    }

}
