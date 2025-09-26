package com.concentrix.asset.repository;

import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.enums.TransactionStatus;
import com.concentrix.asset.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TransactionRepository extends JpaRepository<AssetTransaction, Integer>, JpaSpecificationExecutor<AssetTransaction> {

    Page<AssetTransaction> findALLByTransactionType(TransactionType transactionType, Pageable pageable);

    List<AssetTransaction> findAllByUserUseIsNotNull();

    Page<AssetTransaction> findAllByTransactionTypeAndTransactionStatus(
            TransactionType transactionType,
            TransactionStatus transactionStatus,
            Pageable pageable);

    List<AssetTransaction> findAllByUserUse_Eid(String eid);

    @Query("SELECT DISTINCT t.userUse FROM AssetTransaction t WHERE t.userUse.eid IS NOT NULL")
    List<User> findDistinctEidFromTransactions();

    @Query("SELECT t FROM AssetTransaction t WHERE t.transactionType = 'TRANSFER_SITE' " +
            "AND t.transactionStatus IN ('PENDING', 'APPROVED') ")
    Page<AssetTransaction> findPendingOrApprovedTransfers(Pageable pageable);

}