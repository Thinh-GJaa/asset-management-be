package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.response.LowStockResponse;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.LowStockService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class LowStockServiceImpl implements LowStockService {

    SiteRepository siteRepository;
    DeviceRepository deviceRepository;
    DeviceWarehouseRepository deviceWarehouseRepository;
    DeviceUserRepository deviceUserRepository;
    DeviceFloorRepository deviceFloorRepository;
    TransactionDetailRepository transactionDetailRepository;

    final double LOW_STOCK_THRESHOLD_WITH_SERIAL = 0.04;
    final double LOW_STOCK_THRESHOLD_WITHOUT_SERIAL = 0.05;

    @Override
    public List<LowStockResponse> getLowStockDevices() {
        List<LowStockResponse> result = new ArrayList<>();

        try {
            List<Site> sites = siteRepository.findAll();

            List<DeviceType> typesWithoutSerial = List.of(
                    DeviceType.DONGLE, DeviceType.MOUSE, DeviceType.KEYBOARD,
                    DeviceType.UBIKEY, DeviceType.HEADSET
            );
            List<DeviceType> typesWithSerial = List.of(
                    DeviceType.LAPTOP, DeviceType.MACBOOK, DeviceType.MONITOR
            );

            for (Site site : sites) {
                try {
                    if (site.getSiteName().equalsIgnoreCase("po"))
                        continue;
                    // Xử lý cho loại có serial
                    List<LowStockResponse.LowStockType> lowStockTypes = new ArrayList<>();
                    for (DeviceType type : typesWithSerial) {
                        try {
                            int total = deviceRepository.totalDeviceInUseAndInStock(type);
                            int available = deviceRepository.countAssetInStock(site.getSiteId(), type, null, null, null, null);

                            if (total > 0 && available / (double) total <= LOW_STOCK_THRESHOLD_WITH_SERIAL) {
                                lowStockTypes.add(LowStockResponse.LowStockType.builder()
                                        .type(type)
                                        .total(total)
                                        .available(available)
                                        .build());
                            }
                            log.info("[LOW STOCK SERVICE] Low stock (WITH SERIAL) for siteId {} and deviceType {}: {}-{}",
                                    site.getSiteId(), type, available , (double) total);
                        } catch (Exception e) {
                            log.error("[LOW STOCK SERVICE] Error calculating low stock (WITH SERIAL) for siteId {} and deviceType {}: {}",
                                    site.getSiteId(), type, e.getMessage(), e);
                        }
                    }

                    for (DeviceType type : typesWithoutSerial) {
                        try {
                            int totalInFloor = deviceFloorRepository.sumDeviceBySite_Type_Model(null, type, null);
                            int totalAssigned = deviceUserRepository.sumDeviceAssignedByType_Model(type, null);
                            int totalInStock = deviceWarehouseRepository.sumStockBySite_Type_Model(null, type, null);
                            int totalOnTheMove = transactionDetailRepository.sumOnTheMove(type, null);

                            int total = totalInFloor + totalAssigned + totalInStock + totalOnTheMove;

                            int totalInStockOnSite = deviceWarehouseRepository.sumStockBySite_Type_Model(site.getSiteId(), type, null);

                            if (total > 0 && totalInStockOnSite / (double) total <= LOW_STOCK_THRESHOLD_WITHOUT_SERIAL) {
                                lowStockTypes.add(LowStockResponse.LowStockType.builder()
                                        .type(type)
                                        .total(total)
                                        .available(totalInStockOnSite)
                                        .build());
                            }
                            log.info("[LOW STOCK SERVICE] Low stock (WITHOUT SERIAL) for siteId {} and deviceType {}: {}-{}",
                                    site.getSiteId(), type, totalInStockOnSite, (double) total);
                        } catch (Exception e) {
                            log.error("[LOW STOCK SERVICE] Error calculating low stock (WITHOUT SERIAL) for siteId {} and deviceType {}: {}",
                                    site.getSiteId(), type, e.getMessage(), e);
                        }
                    }
                    if (!lowStockTypes.isEmpty()) {
                        result.add(LowStockResponse.builder()
                                .siteId(site.getSiteId())
                                .siteName(site.getSiteName())
                                .lowStockTypes(lowStockTypes)
                                .build());
                    }
                } catch (Exception e) {
                    log.error("[LOW STOCK SERVICE] Error processing site {}: {}", site.getSiteId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("[LOW STOCK SERVICE] Unexpected error in getLowStockDevices: {}", e.getMessage(), e);
        }

        return result;
    }

}