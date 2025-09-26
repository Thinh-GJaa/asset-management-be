package com.concentrix.asset.service.impl;

import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.entity.Device;
import com.concentrix.asset.entity.TransactionDetail;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.repository.TransactionRepository;
import com.concentrix.asset.service.EmailService;
import com.concentrix.asset.service.ReturnRemindService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ReturnRemindServiceImpl implements ReturnRemindService {

    TransactionRepository transactionRepository;
    EmailService emailService;

    public Map<User, Map<Device, Integer>> calculatePendingReturnsForAllUsers() {
        LocalDate today = LocalDate.now();
        List<AssetTransaction> allTransactions = transactionRepository.findAllByUserUseIsNotNull();

        // 1. Lấy các transaction có returnDate trong khoảng từ hôm nay trở về trước
        // (bao gồm cả quá hạn)
        // và có user/eid để nhắc trả thiết bị
        Map<User, List<AssetTransaction>> userDueTxs = allTransactions.stream()
                .filter(tx -> tx.getReturnDate() != null && !tx.getReturnDate().isAfter(today))
                .collect(Collectors.groupingBy(AssetTransaction::getUserUse));

        Map<User, Map<Device, Integer>> result = new HashMap<>();

        for (Map.Entry<User, List<AssetTransaction>> entry : userDueTxs.entrySet()) {
            User user = entry.getKey();
            List<AssetTransaction> dueTxs = entry.getValue();

            // 2. Kiểm tra xem có nên nhắc user hay không
            // Nhắc trước 3 ngày hoặc từ ngày returnDate trở đi
            boolean shouldRemind = dueTxs.stream()
                    .anyMatch(tx -> {
                        LocalDate returnDate = tx.getReturnDate();
                        LocalDate remindStartDate = returnDate.minusDays(3);
                        return !today.isBefore(remindStartDate);
                    });

            if (!shouldRemind) {
                continue; // Bỏ qua user này nếu chưa đến thời điểm nhắc
            }

            // 3. Tìm min createDate của user
            LocalDate minCreateDate = dueTxs.stream()
                    .map(AssetTransaction::getCreatedAt) // LocalDateTime
                    .filter(Objects::nonNull)
                    .map(LocalDateTime::toLocalDate)
                    .min(LocalDate::compareTo)
                    .orElse(today);

            // 4. Lấy tất cả transaction của user từ minCreateDate đến hôm nay của user
            List<AssetTransaction> periodTxs = allTransactions.stream()
                    .filter(tx -> tx.getUserUse() != null && tx.getUserUse().equals(user))
                    .filter(tx -> tx.getCreatedAt() != null
                            && !tx.getCreatedAt().toLocalDate().isBefore(minCreateDate)
                            && !tx.getCreatedAt().toLocalDate().isAfter(today))
                    .filter(tx -> tx.getTransactionType() == TransactionType.RETURN_FROM_USER ||
                            (tx.getTransactionType() == TransactionType.ASSIGNMENT && tx.getReturnDate() != null))
                    .toList();

            // 5. Gom theo device và tính tổng số lượng còn thiếu
            Map<Device, Integer> assigned = new HashMap<>();
            Map<Device, Integer> returned = new HashMap<>();
            for (AssetTransaction tx : periodTxs) {
                for (TransactionDetail detail : tx.getDetails()) {
                    Device device = detail.getDevice();
                    int qty = detail.getQuantity();
                    if (tx.getTransactionType() == TransactionType.ASSIGNMENT) {
                        assigned.put(device, assigned.getOrDefault(device, 0) + qty);
                    } else if (tx.getTransactionType() == TransactionType.RETURN_FROM_USER) {
                        returned.put(device, returned.getOrDefault(device, 0) + qty);
                    }
                }
            }
            Map<Device, Integer> deviceRemainings = new HashMap<>();
            for (Device device : assigned.keySet()) {
                int remain = assigned.get(device) - returned.getOrDefault(device, 0);
                if (remain > 0)
                    deviceRemainings.put(device, remain);
            }
            if (!deviceRemainings.isEmpty()) {
                result.put(user, deviceRemainings);
            }
        }
        return result;
    }
}