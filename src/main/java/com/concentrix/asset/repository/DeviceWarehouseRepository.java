package com.concentrix.asset.repository;

import com.concentrix.asset.entity.DeviceWarehouse;
import com.concentrix.asset.entity.DeviceWarehouseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceWarehouseRepository extends JpaRepository<DeviceWarehouse, DeviceWarehouseId> {
    Optional<DeviceWarehouse> findByWarehouse_WarehouseIdAndDevice_DeviceId(Integer warehouseId, Integer deviceId);
}