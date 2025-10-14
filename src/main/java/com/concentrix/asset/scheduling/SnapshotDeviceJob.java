package com.concentrix.asset.scheduling;


import com.concentrix.asset.service.EmailService;
import com.concentrix.asset.service.SnapshotDeviceService;
import jakarta.mail.MessagingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // bỏ makeFinal
public class SnapshotDeviceJob {

    SnapshotDeviceService snapshotDeviceService;
    EmailService emailService;

    @NonFinal
    @Value("${app.notification.system-alert-email}")
    String alertSystemEmail;

    public void snapshotDataDevice() throws MessagingException {
        try{
            snapshotDeviceService.snapshotDataDevice();
            log.info("[SCHEDULER] [SUCCESS] Snapshot data device successfully");
        }
        catch (Exception e){
            log.error("[SCHEDULER] [ERROR] Failed to snapshot data device", e);
            String subject = "Snapshot Data Device Error";
            String content = e.getMessage();
            emailService.sendEmail(alertSystemEmail, subject, content, null, null);
        }
    }

}
