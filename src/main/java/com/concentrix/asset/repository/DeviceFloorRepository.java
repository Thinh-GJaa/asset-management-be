package com.concentrix.asset.repository;

import com.concentrix.asset.entity.DeviceFloor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceFloorRepository extends JpaRepository<DeviceFloor, DeviceFloor> {
    Optional<DeviceFloor> findByDevice_DeviceIdAndFloor_FloorId(Integer deviceId, Integer floorId);
}