package com.concentrix.asset.service.impl;

import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.repository.TransactionRepository;
import com.concentrix.asset.service.RemindService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class RemindServiceImpl implements RemindService {

    TransactionRepository transactionRepository;

    @Override
    public Map<User, Map<Device, Integer>> calculatePendingReturnsForAllUsers() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(3);

        log.info("[RETURN-REMIND] Bắt đầu tính toán pending returns, today={}, threshold={}", today, threshold);

        // Lấy cả ASSIGNMENT (có hạn <= threshold) và RETURN_FROM_USER
        List<AssetTransaction> allTransactions =
                transactionRepository.findTransactionForReminder(threshold);

        log.info("[RETURN-REMIND] Lấy được {} transactions từ DB để xử lý", allTransactions.size());

        // Nhóm theo user
        Map<User, List<AssetTransaction>> userTxs = allTransactions.stream()
                .collect(Collectors.groupingBy(AssetTransaction::getUserUse));

        Map<User, Map<Device, Integer>> result = new HashMap<>();
        log.info("[RETURN-REMIND] Có {} user(s) cần kiểm tra pending returns", userTxs.size());

        for (Map.Entry<User, List<AssetTransaction>> entry : userTxs.entrySet()) {
            User user = entry.getKey();
            List<AssetTransaction> txs = entry.getValue();

            log.debug("[RETURN-REMIND] Đang xử lý user={} có {} transactions", user.getEmail(), txs.size());

            Map<Device, Integer> assigned = new HashMap<>();
            Map<Device, Integer> returned = new HashMap<>();

            for (AssetTransaction tx : txs) {
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

            // Tính còn lại = ASSIGNMENT - RETURN
            Map<Device, Integer> remaining = assigned.entrySet().stream()
                    .map(e -> Map.entry(e.getKey(), e.getValue() - returned.getOrDefault(e.getKey(), 0)))
                    .filter(e -> e.getValue() > 0) // chỉ giữ device còn giữ
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (!remaining.isEmpty()) {
                log.info("[RETURN-REMIND] User={} còn {} device(s) pending", user.getEmail(), remaining.size());
                remaining.forEach((dev, qty) ->
                        log.info("   -> Device={} remainQty={}", dev.getDeviceId(), qty));
                result.put(user, remaining);
            } else {
                log.debug("[RETURN-REMIND] User={} không còn device nào pending", user.getEmail());
            }
        }

        log.info("[RETURN-REMIND] Hoàn tất tính toán. Tổng user có pending = {}", result.size());
        return result;
    }

    @Override
    public Map<Site, List<AssetTransaction>> calculateHandoverImageReminder() {
        List<AssetTransaction> transactionList = transactionRepository.findTransactionsWithoutImages();

        return transactionList.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getFromWarehouse().getSite()
                ));
    }
}
