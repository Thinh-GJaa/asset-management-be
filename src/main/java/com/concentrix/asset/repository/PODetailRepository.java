package com.concentrix.asset.repository;

import com.concentrix.asset.entity.PODetail;
import com.concentrix.asset.entity.PODetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PODetailRepository extends JpaRepository<PODetail, PODetailId> {
    PODetail findByDevice_DeviceIdAndDevice_SerialNumberNotNull(Integer deviceId);
}