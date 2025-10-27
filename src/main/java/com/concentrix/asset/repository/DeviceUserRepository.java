package com.concentrix.asset.repository;

import com.concentrix.asset.entity.DeviceUser;
import com.concentrix.asset.entity.DeviceUserId;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceUserRepository extends JpaRepository<DeviceUser, DeviceUserId> {

        Optional<DeviceUser> findByDevice_DeviceIdAndUser_Eid(Integer deviceId, String userEid);

        List<DeviceUser> findAllByUser_Eid(String eid);

        @Query("""
                                SELECT DISTINCT du.user FROM DeviceUser du
                                WHERE du.device.serialNumber IS NULL
                                AND du.quantity > 0
                        """)
        List<User> findUserHavingDevice();

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

        @Query("""
                                SELECT COALESCE(SUM(du.quantity), 0) FROM DeviceUser du
                                WHERE du.device.serialNumber IS NULL
                                AND (du.isWAH IS NULL OR du.isWAH = false)
                                AND du.quantity > 0
                                AND (:type IS NULL OR du.device.model.type = :type)
                                AND (:modelId IS NULL OR du.device.model.modelId = :modelId)
                        """)
        Integer sumDeviceAssignedAccessoriesByType_Model(DeviceType type, Integer modelId);

        @Query("""
                                SELECT COALESCE(SUM(du.quantity), 0) FROM DeviceUser du
                                WHERE du.device.serialNumber IS NULL
                                AND du.isWAH = true
                                AND du.quantity > 0
                                AND (:type IS NULL OR du.device.model.type = :type)
                                AND (:modelId IS NULL OR du.device.model.modelId = :modelId)
                        """)
        Integer sumDeviceWAHAccessoriesByType_Model(DeviceType type, Integer modelId);

        @Query("""
                                SELECT COALESCE(SUM(du.quantity), 0) FROM DeviceUser du
                                WHERE du.device.serialNumber IS NULL
                                AND du.device.status = 'WAH'
                        """)
        Integer sumDeviceWAH();

        @Query("""
                                SELECT COALESCE(SUM(du.quantity), 0) FROM DeviceUser du
                                WHERE du.device.serialNumber IS NULL
                                AND du.device.status = 'ASSIGNED'
                        """)
        Integer sumDeviceAssignedOnly();

        @Query("""
                                SELECT COALESCE(SUM(du.quantity), 0) FROM DeviceUser du
                                WHERE du.device.serialNumber IS NULL
                                AND du.isWAH = true
                                AND du.quantity > 0
                        """)
        Integer sumDeviceWAHAccessories();

        @Query("""
                                SELECT COALESCE(SUM(du.quantity), 0) FROM DeviceUser du
                                WHERE du.device.serialNumber IS NULL
                                AND (du.isWAH IS NULL OR du.isWAH = false)
                                AND du.quantity > 0
                        """)
        Integer sumDeviceAssignedAccessories();

}