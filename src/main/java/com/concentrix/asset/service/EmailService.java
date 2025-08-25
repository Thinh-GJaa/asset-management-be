package com.concentrix.asset.service;

import jakarta.mail.MessagingException;

import java.util.List;

public interface EmailService {

    void sendEmail(String to, String subject, String body, List<String> cc) throws MessagingException;

}
