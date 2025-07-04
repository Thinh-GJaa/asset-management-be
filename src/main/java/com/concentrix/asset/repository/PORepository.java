package com.concentrix.asset.repository;

import com.concentrix.asset.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PORepository extends JpaRepository<PurchaseOrder, String> {

}
