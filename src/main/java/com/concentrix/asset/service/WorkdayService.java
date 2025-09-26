package com.concentrix.asset.service;

import jakarta.mail.MessagingException;

import java.util.Map;

public interface WorkdayService {

    Map<String, Object> importFromWorkday() throws MessagingException;

}
