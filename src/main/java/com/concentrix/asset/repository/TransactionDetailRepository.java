package com.concentrix.asset.repository;

import com.concentrix.asset.entity.TransactionDetail;
import com.concentrix.asset.entity.TransactionDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionDetailRepository extends JpaRepository<TransactionDetail, TransactionDetailId> {

}