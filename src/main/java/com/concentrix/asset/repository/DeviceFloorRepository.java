package com.concentrix.asset.repository;

import com.concentrix.asset.entity.DeviceFloor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceFloorRepository extends JpaRepository<DeviceFloor, DeviceFloor> {

}