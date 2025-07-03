package com.concentrix.asset.repository;

import com.concentrix.asset.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {
    Page<Warehouse> findAll(Pageable pageable);
}
