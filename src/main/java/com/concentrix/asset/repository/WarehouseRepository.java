package com.concentrix.asset.repository;

import com.concentrix.asset.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface WarehouseRepository extends JpaRepository<Warehouse, Integer>, JpaSpecificationExecutor<Warehouse> {
    Page<Warehouse> findAll(Pageable pageable);
    List<Warehouse> findAllBySite_SiteId(Integer siteId);
}
