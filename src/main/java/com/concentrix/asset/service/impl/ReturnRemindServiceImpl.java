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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ReturnRemindServiceImpl implements ReturnRemindService {

    TransactionRepository transactionRepository;
    EmailService emailService;

    @Override
    public Map<User, Map<Device, Integer>> calculatePendingReturnsForAllUsers() {
        LocalDate today = LocalDate.now();
        log.info("[RETURN-REMIND] Bắt đầu tính toán pending returns, today={}, threshold={}", today, today.plusDays(3));

        // Lọc ngay từ DB
        List<AssetTransaction> allTransactions = transactionRepository
                .findAllByUserUseIsNotNullAndReturnDateLessThanEqual(today.plusDays(3));
        log.info("[RETURN-REMIND] Lấy được {} transactions từ DB để xử lý", allTransactions.size());

        Map<User, List<AssetTransaction>> userDueTxs = allTransactions.stream()
                .collect(Collectors.groupingBy(AssetTransaction::getUserUse));
        log.info("[RETURN-REMIND] Có {} user(s) cần kiểm tra pending returns", userDueTxs.size());

        Map<User, Map<Device, Integer>> result = new HashMap<>();

        for (Map.Entry<User, List<AssetTransaction>> entry : userDueTxs.entrySet()) {
            User user = entry.getKey();
            List<AssetTransaction> dueTxs = entry.getValue();
            log.debug("[RETURN-REMIND] Đang xử lý user={} có {} transactions", user.getEmail(), dueTxs.size());

            LocalDate minCreateDate = dueTxs.stream()
                    .map(AssetTransaction::getCreatedAt)
                    .filter(Objects::nonNull)
                    .map(LocalDateTime::toLocalDate)
                    .min(Comparator.naturalOrder())
                    .orElse(today);
            log.debug("[RETURN-REMIND] User={} minCreateDate={}", user.getEmail(), minCreateDate);

            // Filter transaction trong khoảng thời gian
            List<AssetTransaction> periodTxs = dueTxs.stream()
                    .filter(tx -> !tx.getCreatedAt().toLocalDate().isBefore(minCreateDate)
                            && !tx.getCreatedAt().toLocalDate().isAfter(today))
                    .filter(tx -> tx.getTransactionType() == TransactionType.RETURN_FROM_USER
                            || (tx.getTransactionType() == TransactionType.ASSIGNMENT && tx.getReturnDate() != null))
                    .toList();
            log.debug("[RETURN-REMIND] User={} còn {} periodTxs sau khi filter", user.getEmail(), periodTxs.size());

            Map<Device, Integer> assigned = new HashMap<>();
            Map<Device, Integer> returned = new HashMap<>();

            for (AssetTransaction tx : periodTxs) {
                log.trace("[RETURN-REMIND] User={} TxId={} type={} details={}",
                        user.getEmail(), tx.getTransactionId(), tx.getTransactionType(), tx.getDetails().size());
                for (TransactionDetail detail : tx.getDetails()) {
                    Device device = detail.getDevice();
                    int qty = detail.getQuantity();
                    if (tx.getTransactionType() == TransactionType.ASSIGNMENT) {
                        assigned.merge(device, qty, Integer::sum);
                        log.trace("   + Gán device={} qty={} -> totalAssigned={}",
                                device.getDeviceId(), qty, assigned.get(device));
                    } else if (tx.getTransactionType() == TransactionType.RETURN_FROM_USER) {
                        returned.merge(device, qty, Integer::sum);
                        log.trace("   + Trả device={} qty={} -> totalReturned={}",
                                device.getDeviceId(), qty, returned.get(device));
                    }
                }
            }

            Map<Device, Integer> deviceRemainings = assigned.entrySet().stream()
                    .map(e -> Map.entry(e.getKey(), e.getValue() - returned.getOrDefault(e.getKey(), 0)))
                    .filter(e -> e.getValue() > 0)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (!deviceRemainings.isEmpty()) {
                log.info("[RETURN-REMIND] User={} còn {} device(s) pending", user.getEmail(), deviceRemainings.size());
                deviceRemainings.forEach((dev, qty) ->
                        log.info("   -> Device={} remainQty={}", dev.getDeviceId(), qty));
                result.put(user, deviceRemainings);
            } else {
                log.debug("[RETURN-REMIND] User={} không còn device nào pending", user.getEmail());
            }
        }

        log.info("[RETURN-REMIND] Hoàn tất tính toán. Tổng user có pending = {}", result.size());
        return result;
    }

}