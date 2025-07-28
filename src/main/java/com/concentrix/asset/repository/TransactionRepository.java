package com.concentrix.asset.repository;

import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.entity.Device;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.enums.TransactionStatus;
import com.concentrix.asset.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<AssetTransaction, Integer> {

    Page<AssetTransaction> findALLByTransactionType(TransactionType transactionType, Pageable pageable);

    Page<AssetTransaction> findAllByTransactionTypeAndTransactionStatus(
            TransactionType transactionType,
            TransactionStatus transactionStatus,
            Pageable pageable);

}