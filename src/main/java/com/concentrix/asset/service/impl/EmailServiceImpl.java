package com.concentrix.asset.service.impl;

import com.concentrix.asset.exception.EmailException;
import com.concentrix.asset.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.lang.NonNull;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._ -]+$");
    private static final int MAX_FILENAME_LENGTH = 255;

    private final JavaMailSender mailSender;
    private final String defaultFrom;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${spring.mail.default-from:AssetManagementSystem@concentrix.com}") String defaultFrom) {
        this.mailSender = Objects.requireNonNull(mailSender, "mailSender cannot be null");
        this.defaultFrom = StringUtils.defaultIfBlank(defaultFrom, "noreply@concentrix.com");
        log.info("EmailService initialized with default from: {}", this.defaultFrom);
    }

    @Override
    public void sendSimpleMail(@NonNull String to, @NonNull String subject, @NonNull String text) {
        validateEmail(to);
        log.debug("Sending simple email to: {}, subject: {}, text length: {}", to, subject, text.length());
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(defaultFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            log.info("Successfully sent simple email to: {}, subject: {}", to, subject);
        } catch (Exception e) {
            String errorMsg = String.format("Failed to send simple email to %s: %s", to, e.getMessage());
            log.error(errorMsg, e);
            throw new EmailException(errorMsg, e);
        }
    }

    @Override
    public void sendHtmlMail(@NonNull String to, @NonNull String subject, @NonNull String htmlBody) {
        validateEmail(to);
        log.debug("Sending HTML email to: {}, subject: {}, html length: {}", to, subject, htmlBody.length());

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, MimeMessageHelper.MULTIPART_MODE_NO,
                    StandardCharsets.UTF_8.name());
            
            helper.setFrom(defaultFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            mailSender.send(mime);
            log.info("Successfully sent HTML email to: {}, subject: {}", to, subject);
        } catch (MessagingException e) {
            String errorMsg = String.format("Failed to send HTML email to %s: %s", to, e.getMessage());
            log.error(errorMsg, e);
            throw new EmailException(errorMsg, e);
        }
    }

    @Override
    public void sendMailWithAttachments(@NonNull String to, @NonNull String subject, @NonNull String body, boolean html,
            Map<String, byte[]> attachments) {
        validateEmail(to);
        log.debug("Sending email with attachments to: {}, subject: {}, body length: {}, attachment count: {}", 
                to, subject, body.length(), attachments != null ? attachments.size() : 0);

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            
            helper.setFrom(defaultFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, html);

            if (attachments != null && !attachments.isEmpty()) {
                for (Map.Entry<String, byte[]> entry : attachments.entrySet()) {
                    String filename = entry.getKey();
                    validateFilename(filename);
                    
                    byte[] data = entry.getValue();
                    if (data == null || data.length == 0) {
                        log.warn("Skipping empty attachment: {}", filename);
                        continue;
                    }
                    
                    String contentType = MediaTypeFactory.getMediaType(filename)
                            .map(MediaType::toString)
                            .orElse("application/octet-stream");
                            
                    log.debug("Adding attachment: {} ({} bytes, {})", filename, data.length, contentType);
                    helper.addAttachment(filename, new ByteArrayResource(data), contentType);
                }
            }

            mailSender.send(mime);
            log.info("Successfully sent email with attachments to: {}, subject: {}", to, subject);
        } catch (MessagingException e) {
            String errorMsg = String.format("Failed to send email with attachments to %s: %s", to, e.getMessage());
            log.error(errorMsg, e);
            throw new EmailException(errorMsg, e);
        }
    }
    
    private void validateEmail(String email) {
        if (StringUtils.isBlank(email)) {
            throw new EmailException("Email address cannot be empty");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new EmailException("Invalid email address: " + email);
        }
    }
    
    private void validateFilename(String filename) {
        if (StringUtils.isBlank(filename)) {
            throw new EmailException("Attachment filename cannot be empty");
        }
        if (filename.length() > MAX_FILENAME_LENGTH) {
            throw new EmailException("Attachment filename too long: " + filename);
        }
        if (!FILENAME_PATTERN.matcher(filename).matches()) {
            throw new EmailException("Invalid characters in filename: " + filename);
        }
    }
}
