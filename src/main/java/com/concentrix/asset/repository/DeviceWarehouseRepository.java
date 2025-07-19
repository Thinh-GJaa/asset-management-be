package com.concentrix.asset.repository;

import com.concentrix.asset.entity.DeviceWarehouse;
import com.concentrix.asset.entity.DeviceWarehouseId;
import com.concentrix.asset.entity.Model;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceWarehouseRepository extends JpaRepository<DeviceWarehouse, DeviceWarehouseId> {

        Optional<DeviceWarehouse> findByWarehouse_WarehouseIdAndDevice_DeviceId(Integer warehouseId, Integer deviceId);

        // Lấy total device without serial in stock theo site, model, type
        @Query("""
                            SELECT COALESCE(SUM(dw.quantity), 0)
                            FROM DeviceWarehouse dw
                            WHERE (:type IS NULL OR dw.device.model.type = :type)
                              AND (:modelId IS NULL OR dw.device.model.modelId = :modelId)
                              AND (:siteId IS NULL OR dw.warehouse.site.siteId = :siteId)
                        """)
        Integer sumQuantityInStockBySite(
                        @Param("type") com.concentrix.asset.enums.DeviceType type,
                        @Param("modelId") Integer modelId,
                        @Param("siteId") Integer siteId);

        @Query("SELECT COALESCE(SUM(dw.quantity), 0) FROM DeviceWarehouse dw")
        int sumAllStock();

        @Query("""
                            SELECT COALESCE(SUM(dw.quantity), 0) FROM DeviceWarehouse dw
                            WHERE dw.device.serialNumber IS NOT NULL AND dw.device.serialNumber <> ''
                            AND (:type IS NULL OR dw.device.model.type = :type)
                            AND (:modelId IS NULL OR dw.device.model.modelId = :modelId)
                            AND (:siteId IS NULL OR dw.warehouse.site.siteId = :siteId)
                        """)
        Integer sumQuantityInStockWithSerialBySite(@Param("type") com.concentrix.asset.enums.DeviceType type,
                        @Param("modelId") Integer modelId,
                        @Param("siteId") Integer siteId);

        // Tổng quantity in stock cho tất cả site (with serial)
        @Query("""
                            SELECT COALESCE(SUM(dw.quantity), 0) FROM DeviceWarehouse dw
                            WHERE dw.device.serialNumber IS NOT NULL AND dw.device.serialNumber <> ''
                        """)
        Integer sumAllStockWithSerial();
}