package com.concentrix.asset.repository;

import com.concentrix.asset.entity.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Integer> {
    Optional<Device> findBySerialNumber(String serialNumber);

    // Lọc theo modelId
    Page<Device> findByModel_ModelId(Integer modelId, Pageable pageable);

    // Lọc theo DeviceType (type của model)
    Page<Device> findByModel_Type(com.concentrix.asset.enums.DeviceType type, Pageable pageable);
}