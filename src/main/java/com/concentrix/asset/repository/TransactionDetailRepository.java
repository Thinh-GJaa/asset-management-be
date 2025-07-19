package com.concentrix.asset.repository;

import com.concentrix.asset.entity.TransactionDetail;
import com.concentrix.asset.entity.TransactionDetailId;
import com.concentrix.asset.entity.Model;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionDetailRepository extends JpaRepository<TransactionDetail, TransactionDetailId> {

    TransactionDetail findFirstByDevice_DeviceIdOrderByTransaction_TransactionIdDesc(Integer deviceId);

    List<TransactionDetail> findAllByDevice_DeviceIdOrderByTransaction_TransactionIdAsc(Integer deviceId);

    List<TransactionDetail> findAllByDevice_DeviceIdAndTransaction_UserUse_Eid(Integer deviceId, String eid);

    // Lấy total device without serial floor in)
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE (td.device.serialNumber IS NULL)
                  AND (:type IS NULL OR td.device.model.type = :type)
                  AND (:modelId IS NULL OR td.device.model.modelId = :modelId)
                  AND (td.transaction.transactionType = 'USE_FLOOR' or td.transaction.transactionType = 'TRANSFER_FLOOR')
                  AND (:siteId IS NULL OR td.transaction.toFloor.site.siteId = :siteId)
            """)
    Integer sumFloorInBySite(@Param("type") com.concentrix.asset.enums.DeviceType type,
            @Param("modelId") Integer modelId, @Param("siteId") Integer siteId);

    // Lấy total device without serial return_from_floor
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE (td.device.serialNumber IS NULL)
                  AND (:type IS NULL OR td.device.model.type = :type)
                  AND (:modelId IS NULL OR td.device.model.modelId = :modelId)
                  AND (td.transaction.transactionType = 'RETURN_FROM_FLOOR' or td.transaction.transactionType = 'TRANSFER_FLOOR')
                  AND (:siteId IS NULL OR td.transaction.fromFloor.site.siteId = :siteId)
            """)
    Integer sumFloorOutSite(@Param("type") DeviceType type,
            @Param("modelId") Integer modelId, @Param("siteId") Integer siteId);

    // Lấy total device without serial on_the-move
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE (td.device.serialNumber IS NULL)
                  AND (:type IS NULL OR td.device.model.type = :type)
                  AND (:modelId IS NULL OR td.device.model.modelId = :modelId)
                  AND td.transaction.transactionType = 'TRANSFER_SITE'
                  AND td.transaction.transactionStatus = 'PENDING'
                  AND (:siteId IS NULL OR td.transaction.fromWarehouse.site.siteId = :siteId)
            """)
    Integer sumOnTheMove(@Param("type") com.concentrix.asset.enums.DeviceType type,
            @Param("modelId") Integer modelId, @Param("siteId") Integer siteId);

    // Get total device without serial disposal
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE (td.device.serialNumber IS NULL)
                  AND (:type IS NULL OR td.device.model.type = :type)
                  AND (:modelId IS NULL OR td.device.model.modelId = :modelId)
                  AND td.transaction.transactionType = 'DISPOSAL'
                  AND (:siteId IS NULL OR td.transaction.fromWarehouse.site.siteId = :siteId)
            """)
    Integer sumDisposed(@Param("type") com.concentrix.asset.enums.DeviceType type,
            @Param("modelId") Integer modelId, @Param("siteId") Integer siteId);

    // Lấy total device without serial e_waste
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE (td.device.serialNumber IS NULL)
                  AND (:type IS NULL OR td.device.model.type = :type)
                  AND (:modelId IS NULL OR td.device.model.modelId = :modelId)
                  AND td.transaction.transactionType = 'E_WASTE'
                  AND (:siteId IS NULL OR td.transaction.fromWarehouse.site.siteId = :siteId)
            """)
    Integer sumEWaste(@Param("type") DeviceType type,
            @Param("modelId") Integer modelId, @Param("siteId") Integer siteId);

    // Tổng quantity USE_FLOOR cho tất cả site
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE (td.device.serialNumber IS NULL)
                  AND td.transaction.transactionType = 'USE_FLOOR'
            """)
    Integer sumAllUseFloor();

    // Tổng quantity RETURN_FROM_FLOOR cho tất cả site
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE (td.device.serialNumber IS NULL)
                  AND td.transaction.transactionType = 'RETURN_FROM_FLOOR'
            """)
    Integer sumAllReturnFromFloor();

    // Tổng quantity DISPOSAL cho tất cả site
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE (td.device.serialNumber IS NULL)
                  AND td.transaction.transactionType = 'DISPOSAL'
            """)
    Integer sumAllDisposal();

    // Tổng quantity E_WASTE cho tất cả site
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE (td.device.serialNumber IS NULL)
                  AND td.transaction.transactionType = 'E_WASTE'
            """)
    Integer sumAllEWaste();

    // Tổng quantity REPAIR cho tất cả site
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE (td.device.serialNumber IS NULL)
                  AND td.transaction.transactionType = 'REPAIR'
            """)
    Integer sumAllRepair();

    // Tổng quantity RETURN_FROM_REPAIR cho tất cả site
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE (td.device.serialNumber IS NULL)
                  AND td.transaction.transactionType = 'RETURN_FROM_REPAIR'
            """)
    Integer sumAllReturnFromRepair();

    // Tổng quantity ASSIGNMENT cho tất cả site
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE (td.device.serialNumber IS NULL)
                  AND td.transaction.transactionType = 'ASSIGNMENT'
            """)
    Integer sumAllAssignment();

    // Tổng quantity RETURN_FROM_USER cho tất cả site
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE (td.device.serialNumber IS NULL)
                  AND td.transaction.transactionType = 'RETURN_FROM_USER'
            """)
    Integer sumAllReturnFromUser();

    // Lấy total device without serial on_the-move
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE (td.device.serialNumber IS NULL)
                  AND td.transaction.transactionType = 'TRANSFER_SITE'
                  AND td.transaction.transactionStatus = 'PENDING'
            """)
    Integer sumAllOnTheMove();

    // Tổng quantity USE_FLOOR cho tất cả site (with serial)
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE td.device.serialNumber IS NOT NULL AND td.device.serialNumber <> ''
                  AND td.transaction.transactionType = 'USE_FLOOR'
            """)
    Integer sumAllUseFloorWithSerial();

    // Tổng quantity RETURN_FROM_FLOOR cho tất cả site (with serial)
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE td.device.serialNumber IS NOT NULL AND td.device.serialNumber <> ''
                  AND td.transaction.transactionType = 'RETURN_FROM_FLOOR'
            """)
    Integer sumAllReturnFromFloorWithSerial();

    // Tổng quantity DISPOSAL cho tất cả site (with serial)
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE td.device.serialNumber IS NOT NULL AND td.device.serialNumber <> ''
                  AND td.transaction.transactionType = 'DISPOSAL'
            """)
    Integer sumAllDisposalWithSerial();

    // Tổng quantity E_WASTE cho tất cả site (with serial)
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE td.device.serialNumber IS NOT NULL AND td.device.serialNumber <> ''
                  AND td.transaction.transactionType = 'E_WASTE'
            """)
    Integer sumAllEWasteWithSerial();

    // Tổng quantity REPAIR cho tất cả site (with serial)
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE td.device.serialNumber IS NOT NULL AND td.device.serialNumber <> ''
                  AND td.transaction.transactionType = 'REPAIR'
            """)
    Integer sumAllRepairWithSerial();

    // Tổng quantity RETURN_FROM_REPAIR cho tất cả site (with serial)
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE td.device.serialNumber IS NOT NULL AND td.device.serialNumber <> ''
                  AND td.transaction.transactionType = 'RETURN_FROM_REPAIR'
            """)
    Integer sumAllReturnFromRepairWithSerial();

    // Tổng quantity ASSIGNMENT cho tất cả site (with serial)
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE td.device.serialNumber IS NOT NULL AND td.device.serialNumber <> ''
                  AND td.transaction.transactionType = 'ASSIGNMENT'
            """)
    Integer sumAllAssignmentWithSerial();

    // Tổng quantity RETURN_FROM_USER cho tất cả site (with serial)
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE td.device.serialNumber IS NOT NULL AND td.device.serialNumber <> ''
                  AND td.transaction.transactionType = 'RETURN_FROM_USER'
            """)
    Integer sumAllReturnFromUserWithSerial();

    // Tổng quantity ON_THE_MOVE cho tất cả site (with serial)
    @Query("""
                SELECT COALESCE(SUM(td.quantity), 0) FROM TransactionDetail td
                WHERE td.device.serialNumber IS NOT NULL AND td.device.serialNumber <> ''
                  AND td.transaction.transactionType = 'TRANSFER_SITE'
                  AND td.transaction.transactionStatus = 'PENDING'
            """)
    Integer sumAllOnTheMoveWithSerial();

}