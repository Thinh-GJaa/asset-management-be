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

    public Map<User, Map<Device, Integer>> calculatePendingReturnsForAllUsers() {
        LocalDate today = LocalDate.now();

        // Lọc ngay từ DB: chỉ lấy transaction có user và returnDate <= today+3
        List<AssetTransaction> allTransactions = transactionRepository
                .findAllByUserUseIsNotNullAndReturnDateLessThanEqual(today.plusDays(3));

        Map<User, List<AssetTransaction>> userDueTxs = allTransactions.stream()
                .collect(Collectors.groupingBy(AssetTransaction::getUserUse));

        Map<User, Map<Device, Integer>> result = new HashMap<>();

        for (Map.Entry<User, List<AssetTransaction>> entry : userDueTxs.entrySet()) {
            User user = entry.getKey();
            List<AssetTransaction> dueTxs = entry.getValue();

            LocalDate minCreateDate = dueTxs.stream()
                    .map(AssetTransaction::getCreatedAt)
                    .filter(Objects::nonNull)
                    .map(LocalDateTime::toLocalDate)
                    .min(Comparator.naturalOrder())
                    .orElse(today);

            // Filter transaction trong khoảng thời gian
            List<AssetTransaction> periodTxs = dueTxs.stream()
                    .filter(tx -> !tx.getCreatedAt().toLocalDate().isBefore(minCreateDate)
                            && !tx.getCreatedAt().toLocalDate().isAfter(today))
                    .filter(tx -> tx.getTransactionType() == TransactionType.RETURN_FROM_USER
                            || (tx.getTransactionType() == TransactionType.ASSIGNMENT && tx.getReturnDate() != null))
                    .toList();

            Map<Device, Integer> assigned = new HashMap<>();
            Map<Device, Integer> returned = new HashMap<>();

            for (AssetTransaction tx : periodTxs) {
                for (TransactionDetail detail : tx.getDetails()) {
                    Device device = detail.getDevice();
                    int qty = detail.getQuantity();
                    if (tx.getTransactionType() == TransactionType.ASSIGNMENT) {
                        assigned.merge(device, qty, Integer::sum);
                    } else if (tx.getTransactionType() == TransactionType.RETURN_FROM_USER) {
                        returned.merge(device, qty, Integer::sum);
                    }
                }
            }

            Map<Device, Integer> deviceRemainings = assigned.entrySet().stream()
                    .map(e -> Map.entry(e.getKey(), e.getValue() - returned.getOrDefault(e.getKey(), 0)))
                    .filter(e -> e.getValue() > 0)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (!deviceRemainings.isEmpty()) {
                result.put(user, deviceRemainings);
            }
        }
        return result;
    }

}