package com.concentrix.asset.repository;

import com.concentrix.asset.entity.Device;
import com.concentrix.asset.entity.Model;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Integer> {
    Optional<Device> findBySerialNumber(String serialNumber);

    // Lọc theo modelId
    Page<Device> findAllByModel_ModelId(Integer modelId, Pageable pageable);

    // Lọc theo DeviceType (type của model)
    Page<Device> findAllByModel_Type(DeviceType type, Pageable pageable);

    Optional<Device> findFirstByModel_ModelId(Integer modelId);

    Integer countByStatusAndSerialNumberIsNotNull(DeviceStatus status);

    @Query("""
                SELECT COUNT(d) FROM Device d
                WHERE (:type IS NULL OR d.model.type = :type)
                  AND (:siteId IS NULL OR d.currentWarehouse.site.siteId = :siteId)
                  AND (:modelId IS NULL OR d.model.modelId = :modelId)
                  AND d.status = 'IN_STOCK'
                  AND d.serialNumber IS NOT NULL
            """)
    int countAssetInStock(
            @Param("siteId") Integer siteId,
            @Param("type") DeviceType type,
            @Param("modelId") Integer modelId);

    @Query("""
                SELECT COUNT(d) FROM Device d
                WHERE (:type IS NULL OR d.model.type = :type)
                  AND (:siteId IS NULL OR d.currentFloor.site.siteId = :siteId)
                  AND (:floorId IS NULL OR d.currentFloor.floorId = :floorId)
                  AND (:modelId IS NULL OR d.model.modelId = :modelId)
                  AND d.status = 'IN_FLOOR'
                  AND d.serialNumber IS NOT NULL
            """)
    int countAssetInFloor(
            @Param("siteId") Integer siteId,
            @Param("floorId") Integer floorId,
            @Param("type") DeviceType type,
            @Param("modelId") Integer modelId);

    @Query("""
                SELECT COUNT(d) FROM Device d
                WHERE (:type IS NULL OR d.model.type = :type)
                  AND (:modelId IS NULL OR d.model.modelId = :modelId)
                  AND d.status = 'ON_THE_MOVE'
                  AND d.serialNumber IS NOT NULL
            """)
    int countAssetOnTheMove(
            @Param("type") DeviceType type,
            @Param("modelId") Integer modelId);

    @Query("""
                SELECT COUNT(d) FROM Device d
                WHERE d.status = :status
                  AND (:type IS NULL OR d.model.type = :type)
                  AND (:modelId IS NULL OR d.model.modelId = :modelId)
                  AND d.serialNumber IS NOT NULL
            """)
    int countAssetByStatus(
            @Param("status") DeviceStatus status,
            @Param("type") DeviceType type,
            @Param("modelId") Integer modelId);


    @Query("""
        SELECT d FROM Device d
        WHERE d.status = 'IN_STOCK'
            AND (:modelId IS NULL OR d.model.modelId = :modelId)
            AND (:type IS NULL OR d.model.type = :type)
            AND (:siteId IS NULL OR d.currentWarehouse.site.siteId = :siteId)
            AND (:siteId IS NULL OR d.currentFloor.site.siteId = :siteId)
            AND d.serialNumber IS NOT NULL

        """)
    List<Device> findDevicesInStockForReport(
            @Param("siteId") Integer siteId,
            @Param("type") DeviceType type,
            @Param("modelId") Integer modelId);

    @Query("""
        SELECT d FROM Device d
        WHERE d.status = 'IN_FLOOR'
            AND (:modelId IS NULL OR d.model.modelId = :modelId)
            AND (:type IS NULL OR d.model.type = :type)
            AND (:floorId IS NULL OR d.currentFloor.floorId = :floorId)
            AND (:siteId IS NULL OR d.currentFloor.site.siteId = :siteId)
            AND d.serialNumber IS NOT NULL

        """)
    List<Device> findDevicesInFloorForReport(
            @Param("siteId") Integer siteId,
            @Param("floorId") Integer floorId,
            @Param("type") DeviceType type,
            @Param("modelId") Integer modelId);

     @Query("""
        SELECT d FROM Device d
        WHERE d.status = 'ON_THE_MOVE'
            AND (:modelId IS NULL OR d.model.modelId = :modelId)
            AND (:type IS NULL OR d.model.type = :type)
            AND d.serialNumber IS NOT NULL

        """)
    List<Device> findDevicesOnTheMoveForReport(
            @Param("type") DeviceType type,
            @Param("modelId") Integer modelId);

     @Query("""
        SELECT d FROM Device d
        WHERE d.status = :status
            AND (:modelId IS NULL OR d.model.modelId = :modelId)
            AND (:type IS NULL OR d.model.type = :type)
            AND d.serialNumber IS NOT NULL
        """)
    List<Device> findDevicesStatusForReport(
            @Param("status") DeviceStatus status,
            @Param("type") DeviceType type,
            @Param("modelId") Integer modelId);

}