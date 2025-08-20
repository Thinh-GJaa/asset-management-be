package com.concentrix.asset.repository;

import com.concentrix.asset.entity.DeviceUser;
import com.concentrix.asset.entity.DeviceUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceUserRepository extends JpaRepository<DeviceUser, DeviceUserId> {
        Optional<DeviceUser> findByDevice_DeviceIdAndUser_Eid(Integer deviceId, String userEid);
}