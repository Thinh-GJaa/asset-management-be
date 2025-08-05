package com.concentrix.asset.repository;

import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.enums.TransactionStatus;
import com.concentrix.asset.enums.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Repository
public interface TransactionRepository extends JpaRepository<AssetTransaction, Integer>, JpaSpecificationExecutor<AssetTransaction> {

    /**
     * Dynamic filter for transactionId, fromDate, toDate, transactionType
     */
    default Page<AssetTransaction> findALLByTransactionTypeAndDynamicFilter(
            TransactionType transactionType,
            Integer transactionId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        return findAll((root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (transactionType != null) {
                predicates.add(cb.equal(root.get("transactionType"), transactionType));
            }
            if (transactionId != null) {
                predicates.add(cb.equal(root.get("transactionId"), transactionId));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    Page<AssetTransaction> findALLByTransactionType(TransactionType transactionType, Pageable pageable);

    Page<AssetTransaction> findAllByTransactionTypeAndTransactionStatus(
            TransactionType transactionType,
            TransactionStatus transactionStatus,
            Pageable pageable);

    List<AssetTransaction> findAllByUserUse_Eid(String eid);

    @Query("SELECT DISTINCT t.userUse FROM AssetTransaction t WHERE t.userUse.eid IS NOT NULL")
    List<User> findDistinctEidFromTransactions();

}