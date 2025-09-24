package com.concentrix.asset.controller;

import com.concentrix.asset.scheduling.LowStockNotificationJob;
import com.concentrix.asset.scheduling.RemindReturnAssetJob;
import com.concentrix.asset.service.ReturnRemindService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/test")
public class TestController{

    LowStockNotificationJob lowStockNotificationJob;
    RemindReturnAssetJob remindReturnAssetJob;

    @GetMapping("/low-stock")
    public void testLowStock() {
        lowStockNotificationJob.sendLowStockNotifications();
    }

    @GetMapping("/return-reminders")
    public void testReturnReminders() {
        remindReturnAssetJob.sendReturnReminders();
    }

    @GetMapping("/time")
    public String testTime() {
        return LocalDateTime.now().toString();
    }

}