package com.concentrix.asset.repository;

import com.concentrix.asset.entity.Device;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Integer>, JpaSpecificationExecutor<Device> {

  Optional<Device> findBySerialNumber(String serialNumber);

  Optional<Device> findBySerialNumberContaining(String serialNumber);

  Optional<Device> findByHostName(String hostName);

  List<Device> findAllByCurrentUser_Eid(String eid);

  Optional<Device> findFirstByModel_ModelId(Integer modelId);

  Integer countByStatusAndSerialNumberIsNotNull(DeviceStatus status);

  List<Device> findAllBySerialNumberIsNotNull();

  Optional<Device> findBySeatNumber(String seatNumber);

  @Query("""
          SELECT d FROM Device d
          WHERE
              LOWER(d.serialNumber) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(d.hostName) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(d.seatNumber) LIKE LOWER(CONCAT('%', :q, '%'))
      """)
  List<Device> searchDevices(@Param("q") String q);

  @Query("""
          select distinct u
          from User u
          where u.eid in (
              select d.currentUser.eid
              from Device d
              where d.currentUser is not null
                      and d.serialNumber is not null
          )
          or u.eid in (
              select du.user.eid from DeviceUser du where du.quantity > 0
          )
      """)
  Page<User> findUsersWithDevices(Pageable pageable);

  @Query("""
          SELECT COUNT(d)
          FROM Device d
          WHERE d.status = 'IN_STOCK'
            AND d.serialNumber IS NOT NULL

            AND (:type IS NULL OR d.model.type = :type)
            AND (:siteId IS NULL OR d.currentWarehouse.site.siteId = :siteId)
            AND (:modelId IS NULL OR d.model.modelId = :modelId)
            AND (
                  :isOutOfWarranty IS NULL
               OR (:isOutOfWarranty = true  AND (d.endDate IS NULL OR d.endDate > CURRENT_DATE))
               OR (:isOutOfWarranty = false AND d.endDate IS NOT NULL AND d.endDate <= CURRENT_DATE)
            )
            AND (
              (:startDate IS NULL AND :endDate IS NULL)
              OR (:startDate IS NOT NULL AND :endDate IS NULL AND d.startDate > :startDate)
              OR (:startDate IS NULL AND :endDate IS NOT NULL AND d.startDate <= :endDate)
              OR (:startDate IS NOT NULL AND :endDate IS NOT NULL AND d.startDate BETWEEN :startDate AND :endDate)
              )

      """)
  int countAssetInStock(
      @Param("siteId") Integer siteId,
      @Param("type") DeviceType type,
      @Param("modelId") Integer modelId,
      @Param("isOutOfWarranty") Boolean isOutOfWarranty,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query("""
          SELECT COUNT(d)
          FROM Device d
          WHERE (:type IS NULL OR d.model.type = :type)
            AND (:siteId IS NULL OR d.currentWarehouse.site.siteId = :siteId)
            AND (:modelId IS NULL OR d.model.modelId = :modelId)
            AND d.status = 'E_WASTE'
            AND d.serialNumber IS NOT NULL
            AND (
                  :isOutOfWarranty IS NULL
               OR (:isOutOfWarranty = true
                   AND (d.endDate IS NULL OR d.endDate > CURRENT_DATE))
               OR (:isOutOfWarranty = false
                   AND d.endDate IS NOT NULL AND d.endDate <= CURRENT_DATE)
            )
            AND (
              (:startDate IS NULL AND :endDate IS NULL)
              OR (:startDate IS NOT NULL AND :endDate IS NULL AND d.startDate > :startDate)
              OR (:startDate IS NULL AND :endDate IS NOT NULL AND d.startDate <= :endDate)
              OR (:startDate IS NOT NULL AND :endDate IS NOT NULL AND d.startDate BETWEEN :startDate AND :endDate)
              )

      """)
  int countAssetEWaste(
      @Param("siteId") Integer siteId,
      @Param("type") DeviceType type,
      @Param("modelId") Integer modelId,
      @Param("isOutOfWarranty") Boolean isOutOfWarranty,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query("""
          SELECT COUNT(d) FROM Device d
          LEFT JOIN d.currentFloor cf
          LEFT JOIN cf.site s
          LEFT JOIN cf.account a
          LEFT JOIN a.owner o
          LEFT JOIN d.model m
          WHERE (:siteId IS NULL OR s.siteId = :siteId)
            AND d.status = 'IN_FLOOR'
            AND d.serialNumber IS NOT NULL
            AND (:ownerId IS NULL OR o.eid = :ownerId)
            AND (:accountId IS NULL OR a.accountId = :accountId)
            AND (:floorId IS NULL OR cf.floorId = :floorId)
            AND (:type IS NULL OR m.type = :type)
            AND (:modelId IS NULL OR m.modelId = :modelId)
            AND (
              :isOutOfWarranty IS NULL
              OR (:isOutOfWarranty = true AND (d.endDate IS NULL OR d.endDate > CURRENT_DATE))
              OR (:isOutOfWarranty = false AND d.endDate IS NOT NULL AND d.endDate <= CURRENT_DATE)
              )
            AND (
              (:startDate IS NULL AND :endDate IS NULL)
              OR (:startDate IS NOT NULL AND :endDate IS NULL AND d.startDate > :startDate)
              OR (:startDate IS NULL AND :endDate IS NOT NULL AND d.startDate <= :endDate)
              OR (:startDate IS NOT NULL AND :endDate IS NOT NULL AND d.startDate BETWEEN :startDate AND :endDate)
              )

      """)
  int countAssetInFloor(
      @Param("siteId") Integer siteId,
      @Param("ownerId") Integer ownerId,
      @Param("accountId") Integer accountId,
      @Param("floorId") Integer floorId,
      @Param("type") DeviceType type,
      @Param("modelId") Integer modelId,
      @Param("isOutOfWarranty") Boolean isOutOfWarranty,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query("""
          SELECT COUNT(d) FROM Device d
          WHERE d.status = :status
            AND (:type IS NULL OR d.model.type = :type)
            AND (:modelId IS NULL OR d.model.modelId = :modelId)
            AND d.serialNumber IS NOT NULL
            AND (
                :isOutOfWarranty IS NULL
                OR (:isOutOfWarranty = true AND (d.endDate IS NULL OR d.endDate > CURRENT_DATE))
                OR (:isOutOfWarranty = false AND d.endDate IS NOT NULL AND d.endDate <= CURRENT_DATE)
              )
           AND (
              (:startDate IS NULL AND :endDate IS NULL)
              OR (:startDate IS NOT NULL AND :endDate IS NULL AND d.startDate > :startDate)
              OR (:startDate IS NULL AND :endDate IS NOT NULL AND d.startDate <= :endDate)
              OR (:startDate IS NOT NULL AND :endDate IS NOT NULL AND d.startDate BETWEEN :startDate AND :endDate)
              )

      """)
  int countAssetByStatus(
      @Param("status") DeviceStatus status,
      @Param("type") DeviceType type,
      @Param("modelId") Integer modelId,
      @Param("isOutOfWarranty") Boolean isOutOfWarranty,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query("""
          SELECT d FROM Device d
          WHERE d.status = 'IN_STOCK'
            AND (:modelId IS NULL OR d.model.modelId = :modelId)
            AND (:type IS NULL OR d.model.type = :type)
            AND (:siteId IS NULL OR d.currentWarehouse.site.siteId = :siteId)
            AND d.serialNumber IS NOT NULL
            AND (:isOutOfWarranty IS NULL
                  OR (
                      (:isOutOfWarranty = TRUE AND d.endDate IS NOT NULL AND d.endDate < CURRENT_DATE)
                      OR
                      (:isOutOfWarranty = FALSE AND (d.endDate IS NULL OR d.endDate >= CURRENT_DATE))
                  )
                )
              AND (
                      (:startDate IS NULL AND :endDate IS NULL)
                      OR (:startDate IS NOT NULL AND :endDate IS NULL AND d.startDate > :startDate)
                      OR (:startDate IS NULL AND :endDate IS NOT NULL AND d.startDate <= :endDate)
                      OR (:startDate IS NOT NULL AND :endDate IS NOT NULL AND d.startDate BETWEEN :startDate AND :endDate)
              )


      """)
  List<Device> findDevicesInStockForReport(
      @Param("siteId") Integer siteId,
      @Param("type") DeviceType type,
      @Param("modelId") Integer modelId,
      @Param("isOutOfWarranty") Boolean isOutOfWarranty,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query("""
          SELECT d FROM Device d
          WHERE d.status = 'E_WASTE'
            AND (:modelId IS NULL OR d.model.modelId = :modelId)
            AND (:type IS NULL OR d.model.type = :type)
            AND (:siteId IS NULL OR d.currentWarehouse.site.siteId = :siteId)
            AND d.serialNumber IS NOT NULL
            AND (
                 :isOutOfWarranty IS NULL
                 OR (:isOutOfWarranty = true AND (d.endDate IS NULL OR d.endDate > CURRENT_DATE))
                 OR (:isOutOfWarranty = false AND d.endDate IS NOT NULL AND d.endDate <= CURRENT_DATE)
           )
           AND (
              (:startDate IS NULL AND :endDate IS NULL)
              OR (:startDate IS NOT NULL AND :endDate IS NULL AND d.startDate > :startDate)
              OR (:startDate IS NULL AND :endDate IS NOT NULL AND d.startDate <= :endDate)
              OR (:startDate IS NOT NULL AND :endDate IS NOT NULL AND d.startDate BETWEEN :startDate AND :endDate)
              )

      """)
  List<Device> findDevicesEWasteForReport(
      @Param("siteId") Integer siteId,
      @Param("type") DeviceType type,
      @Param("modelId") Integer modelId,
      @Param("isOutOfWarranty") Boolean isOutOfWarranty,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query("""
          SELECT d
          FROM Device d
          LEFT JOIN d.model m
          LEFT JOIN d.currentFloor cf
          LEFT JOIN cf.site s
          LEFT JOIN cf.account a
          LEFT JOIN a.owner o
          WHERE d.status = 'IN_FLOOR'
            AND d.serialNumber IS NOT NULL

            AND (:modelId IS NULL OR m.modelId = :modelId)
            AND (:type IS NULL OR m.type = :type)
            AND (:floorId IS NULL OR cf.floorId = :floorId)
            AND (:ownerId IS NULL OR o.eid = :ownerId)
            AND (:accountId IS NULL OR a.accountId = :accountId)
            AND (:siteId IS NULL OR s.siteId = :siteId)
            AND (
                :isOutOfWarranty IS NULL
                OR (:isOutOfWarranty = true AND (d.endDate IS NULL OR d.endDate > CURRENT_DATE))
                OR (:isOutOfWarranty = false AND d.endDate IS NOT NULL AND d.endDate <= CURRENT_DATE)
            )
           AND (
              (:startDate IS NULL AND :endDate IS NULL)
              OR (:startDate IS NOT NULL AND :endDate IS NULL AND d.startDate > :startDate)
              OR (:startDate IS NULL AND :endDate IS NOT NULL AND d.startDate <= :endDate)
              OR (:startDate IS NOT NULL AND :endDate IS NOT NULL AND d.startDate BETWEEN :startDate AND :endDate)
              )

      """)
  List<Device> findDevicesInFloorForReport(
      @Param("siteId") Integer siteId,
      @Param("floorId") Integer floorId,
      @Param("ownerId") Integer ownerId,
      @Param("accountId") Integer accountId,
      @Param("type") DeviceType type,
      @Param("modelId") Integer modelId,
      @Param("isOutOfWarranty") Boolean isOutOfWarranty,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query("""
                                                        SELECT d FROM Device d
                                                        WHERE d.status = :status
                                                            AND (:modelId IS NULL OR d.model.modelId = :modelId)
                                                            AND (:type IS NULL OR d.model.type = :type)
                                                            AND d.serialNumber IS NOT NULL
                                                            AND (
                                                               :isOutOfWarranty IS NULL
                                                               OR (:isOutOfWarranty = true AND (d.endDate IS NULL OR d.endDate > CURRENT_DATE))
                                                               OR (:isOutOfWarranty = false AND d.endDate IS NOT NULL AND d.endDate <= CURRENT_DATE)
                                                            )
                                                           AND (
          (:startDate IS NULL AND :endDate IS NULL)
          OR (:startDate IS NOT NULL AND :endDate IS NULL AND d.startDate > :startDate)
          OR (:startDate IS NULL AND :endDate IS NOT NULL AND d.startDate <= :endDate)
          OR (:startDate IS NOT NULL AND :endDate IS NOT NULL AND d.startDate BETWEEN :startDate AND :endDate)
      )

      """)
  List<Device> findDevicesStatusForReport(
      @Param("status") DeviceStatus status,
      @Param("type") DeviceType type,
      @Param("modelId") Integer modelId,
      @Param("isOutOfWarranty") Boolean isOutOfWarranty,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query("""
          SELECT d FROM Device d
          LEFT JOIN d.model m
          LEFT JOIN d.currentFloor cf
          LEFT JOIN cf.site s
          LEFT JOIN cf.account a
          LEFT JOIN a.owner o
          WHERE d.serialNumber IS NOT NULL
            AND (:modelId IS NULL OR m.modelId = :modelId)
            AND (:type IS NULL OR m.type = :type)
            AND (:siteId IS NULL OR s.siteId = :siteId)
            AND (:floorId IS NULL OR cf.floorId = :floorId)
            AND (:ownerId IS NULL OR o.eid = :ownerId)
            AND (:accountId IS NULL OR a.accountId = :accountId)
            AND (
                :isOutOfWarranty IS NULL
                OR (:isOutOfWarranty = true AND (d.endDate IS NULL OR d.endDate > CURRENT_DATE))
                OR (:isOutOfWarranty = false AND d.endDate IS NOT NULL AND d.endDate <= CURRENT_DATE)
            )
            AND (
              (:startDate IS NULL AND :endDate IS NULL)
              OR (:startDate IS NOT NULL AND :endDate IS NULL AND d.startDate > :startDate)
              OR (:startDate IS NULL AND :endDate IS NOT NULL AND d.startDate <= :endDate)
              OR (:startDate IS NOT NULL AND :endDate IS NOT NULL AND d.startDate BETWEEN :startDate AND :endDate)
            )
      """)
  List<Device> findAllDevicesForReport(
      @Param("siteId") Integer siteId,
      @Param("floorId") Integer floorId,
      @Param("ownerId") Integer ownerId,
      @Param("accountId") Integer accountId,
      @Param("type") DeviceType type,
      @Param("modelId") Integer modelId,
      @Param("isOutOfWarranty") Boolean isOutOfWarranty,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query("""
          SELECT COUNT(d)
              FROM Device d
                  WHERE d.model.type = :type
                  AND d.status IN ('IN_STOCK', 'IN_FLOOR', 'ON_THE_MOVE', 'ASSIGNED', 'WAH')
                  AND d.serialNumber IS NOT NULL
      """)
  Integer totalDeviceInUseAndInStock(DeviceType type);

  /**
   * Kiểm tra xem serial number đã tồn tại chưa
   * 
   * @param serialNumber Số serial cần kiểm tra
   * @return true nếu đã tồn tại, false nếu chưa
   */
  boolean existsBySerialNumber(String serialNumber);

  /**
   * Tìm kiếm thiết bị theo trạng thái
   * 
   * @param status Trạng thái thiết bị
   * @return Danh sách thiết bị có trạng thái tương ứng
   */
  List<Device> findByStatus(DeviceStatus status);

  /**
   * Đếm số lượng thiết bị theo trạng thái
   * 
   * @param status Trạng thái thiết bị
   * @return Số lượng thiết bị có trạng thái tương ứng
   */
  long countByStatus(DeviceStatus status);

  /**
   * Tìm kiếm thiết bị theo site thông qua warehouse hoặc floor
   * 
   * @param siteId ID của site
   * @return Danh sách thiết bị thuộc site
   */
  @Query("""
          SELECT d FROM Device d WHERE
          (d.currentWarehouse IS NOT NULL AND d.currentWarehouse.site.siteId = :siteId) OR
          (d.currentFloor IS NOT NULL AND d.currentFloor.site.siteId = :siteId)
      """)
  List<Device> findBySite(@Param("siteId") Integer siteId);

}