package com.concentrix.asset.repository;

import com.concentrix.asset.entity.TransactionDetail;
import com.concentrix.asset.entity.TransactionDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionDetailRepository extends JpaRepository<TransactionDetail, TransactionDetailId> {

    TransactionDetail findFirstByDevice_DeviceIdOrderByTransaction_TransactionIdDesc(Integer deviceId);

    List<TransactionDetail> findAllByDevice_DeviceIdOrderByTransaction_TransactionIdAsc(Integer deviceId);

    List<TransactionDetail> findAllByDevice_DeviceIdAndTransaction_UserUse_Eid(Integer deviceId, String eid);
}