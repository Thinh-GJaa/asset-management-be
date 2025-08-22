package com.concentrix.asset.service;

import java.util.Map;

/**
 * EmailService provides simple APIs to send emails.
 * Requires spring.mail.* to be configured in application properties.
 */
public interface EmailService {

    /**
     * Send a simple text email.
     */
    void sendSimpleMail(String to, String subject, String text);

    /**
     * Send an HTML email.
     */
    void sendHtmlMail(String to, String subject, String htmlBody);

    /**
     * Send an email with attachments. The attachments map keys are filenames and values are raw bytes.
     * Content type will be best-effort based on filename, or application/octet-stream.
     */
    void sendMailWithAttachments(String to, String subject, String body, boolean html,
                                 Map<String, byte[]> attachments);
}
