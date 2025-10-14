package com.concentrix.asset.repository;

import com.concentrix.asset.entity.SnapshotDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SnapshotDeviceRepository
        extends JpaRepository<SnapshotDevice, Integer>, JpaSpecificationExecutor<SnapshotDevice> {

    /**
     * Lấy danh sách các ngày snapshot duy nhất, sắp xếp giảm dần
     *
     * @return List các ngày snapshot
     */
    @Query("SELECT DISTINCT s.snapshotDate FROM SnapshotDevice s ORDER BY s.snapshotDate DESC")
    List<LocalDate> findDistinctSnapshotDatesOrderByDesc();
}