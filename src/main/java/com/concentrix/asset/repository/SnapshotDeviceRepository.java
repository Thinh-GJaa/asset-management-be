package com.concentrix.asset.repository;

import com.concentrix.asset.entity.SnapshotDevice;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SnapshotDeviceRepository
                extends JpaRepository<SnapshotDevice, Integer>, JpaSpecificationExecutor<SnapshotDevice> {

        @Query("""
                        SELECT sd FROM SnapshotDevice sd
                        WHERE sd.snapshotDate = :date
                          AND (:siteId IS NULL
                               OR :status NOT IN ('E_WASTE', 'IN_STOCK', 'IN_FLOOR')
                               OR (sd.site IS NOT NULL AND sd.site.siteId = :siteId))
                          AND (:status IS NULL OR sd.status = :status)
                          AND (:type IS NULL OR sd.device.model.type = :type)
                        """)
        List<SnapshotDevice> getSnapshotDevicesByDate(
                        @Param("date") LocalDate date,
                        @Param("siteId") Integer siteId,
                        @Param("status") DeviceStatus status,
                        @Param("type") DeviceType type);

}