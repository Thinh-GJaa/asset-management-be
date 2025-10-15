package com.concentrix.asset.controller;

import com.concentrix.asset.scheduling.*;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/test")
public class TestController {

    LowStockNotificationJob lowStockNotificationJob;
    RemindReturnAssetJob remindReturnAssetJob;
    UpdateWorkdayJob updateWorkdayJob;
    ReminderUploadHandoverImageJob reminderUploadHandoverImageJob;
    SnapshotDeviceJob snapshotDeviceJob;

    @GetMapping("/low-stock")
    public void testLowStock() throws MessagingException {
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

    @GetMapping("/update-workday")
    public void testUpdateWorkday() throws MessagingException {
        updateWorkdayJob.updateWorkday();
    }

    @GetMapping("/reminder-upload-handover-image")
    public void testReminderUploadHandoverImage() throws MessagingException {
        reminderUploadHandoverImageJob.sendReminderUploadHandoverImage();
    }

    @GetMapping("/snapshot-data")
    public void testSnapshotData() throws MessagingException {
        snapshotDeviceJob.snapshotDataDevice();
    }

}