package com.concentrix.asset.repository;

import com.concentrix.asset.entity.DeviceUser;
import com.concentrix.asset.entity.DeviceUserId;
import com.concentrix.asset.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceUserRepository extends JpaRepository<DeviceUser, DeviceUserId> {

        Optional<DeviceUser> findByDevice_DeviceIdAndUser_Eid(Integer deviceId, String userEid);

        @Query("""
                SELECT COALESCE(SUM(du.quantity), 0) FROM DeviceUser du
                WHERE du.device.serialNumber IS NULL
        """)
        Integer sumDeviceAssigned();

        @Query("""
                SELECT COALESCE(SUM(du.quantity), 0) FROM DeviceUser du
                        WHERE du.device.serialNumber IS NULL
                                AND (:type IS NULL OR du.device.model.type = :type)
                                AND (:modelId IS NULL OR du.device.model.modelId = :modelId)
        """)
        Integer sumDeviceAssignedByType_Model(DeviceType type, Integer modelId);
}