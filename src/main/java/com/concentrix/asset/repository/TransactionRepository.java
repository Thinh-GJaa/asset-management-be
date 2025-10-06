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
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface TransactionRepository extends JpaRepository<AssetTransaction, Integer>, JpaSpecificationExecutor<AssetTransaction> {

    List<AssetTransaction> findAllByUserUse_Eid(String eid);

    @Query("SELECT t FROM AssetTransaction t WHERE t.transactionType = 'TRANSFER_SITE' " +
            "AND t.transactionStatus IN ('PENDING', 'APPROVED') ")
    Page<AssetTransaction> findPendingOrApprovedTransfers(Pageable pageable);

    @Query("""
           SELECT t FROM AssetTransaction t
           WHERE t.userUse IS NOT NULL
             AND (
                  (t.transactionType = TransactionType.ASSIGNMENT
                       AND t.returnDate <= :date)
                  OR t.transactionType = RETURN_FROM_USER
                 )
           """)
    List<AssetTransaction> findTransactionForReminder(@Param("date") LocalDate date);

    @Query("""
            SELECT t
            FROM AssetTransaction t
            WHERE t.images IS EMPTY
                AND t.transactionType in ('ASSIGNMENT', 'RETURN_FROM_USER')
            """)
    List<AssetTransaction> findTransactionsWithoutImages();

}