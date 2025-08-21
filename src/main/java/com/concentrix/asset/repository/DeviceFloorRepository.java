package com.concentrix.asset.repository;

import com.concentrix.asset.entity.Device;
import com.concentrix.asset.entity.DeviceFloor;
import com.concentrix.asset.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceFloorRepository extends JpaRepository<DeviceFloor, DeviceFloor> {
    Optional<DeviceFloor> findByDevice_DeviceIdAndFloor_FloorId(Integer deviceId, Integer floorId);

    @Query("""
        SELECT COALESCE(SUM(df.quantity), 0) FROM DeviceFloor df
        WHERE df.device.serialNumber IS NULL
     """)
    Integer sumDeviceInFloor();

    @Query("""
        SELECT COALESCE(SUM(df.quantity), 0) FROM DeviceFloor df
        WHERE df.device.serialNumber IS NULL
          AND (:siteId IS NULL OR df.floor.site.siteId = :siteId)
          AND (:type IS NULL OR df.device.model.type = :type)
          AND (:modelId IS NULL OR df.device.model.modelId = :modelId)
     """)
    Integer sumDeviceBySite_Type_Model(Integer siteId, DeviceType type, Integer modelId);
}