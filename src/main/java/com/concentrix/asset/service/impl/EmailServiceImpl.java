package com.concentrix.asset.service.impl;

import com.concentrix.asset.service.EmailService;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailServiceImpl implements EmailService {

    JavaMailSender mailSender;

    @Override
    @Async
    @Retryable(
            retryFor = {MessagingException.class, MailException.class, RuntimeException.class},
            maxAttempts = 4,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendEmail(String to,
                          String subject,
                          String body,
                          List<String> cc,
                          List<String> bcc) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("itvn_noreply@concentrix.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            if (cc != null && !cc.isEmpty()) {
                helper.setCc(cc.toArray(new String[0]));
            }

            if (bcc != null && !bcc.isEmpty()) {
                helper.setBcc(bcc.toArray(new String[0]));
            }

            //mailSender.send(message);
            log.info("[EMAIL][SUCCESS] Email sent successfully to [{}] with subject [{}], cc [{}], bcc [{}]", to, subject, cc, bcc);

        } catch (AuthenticationFailedException e) {
            log.error("[EMAIL][AUTH_ERROR] Authentication failed for recipient [{}]. Error: {}", to, e.getMessage(), e);
            throw e; // để Retryable xử lý retry
        } catch (MessagingException | MailException e) {
            log.error("[EMAIL][SEND_ERROR] Failed to send email to [{}]. Error: {}", to, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("[EMAIL][UNEXPECTED_ERROR] Unexpected error while sending email to [{}]. Error: {}", to, e.getMessage(), e);
            throw new RuntimeException("Unable to send email: " + e.getMessage(), e);
        }
    }
}
