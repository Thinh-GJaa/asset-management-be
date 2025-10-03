package com.concentrix.asset.repository;

import com.concentrix.asset.entity.TransactionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionImageRepository extends JpaRepository<TransactionImage, Integer>, JpaSpecificationExecutor<TransactionImage> {


}