package com.concentrix.asset.service;

import com.concentrix.asset.dto.response.LowStockResponse;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.impl.LowStockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LowStockServiceTest {

    @Mock private SiteRepository siteRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private DeviceWarehouseRepository deviceWarehouseRepository;
    @Mock private DeviceUserRepository deviceUserRepository;
    @Mock private DeviceFloorRepository deviceFloorRepository;
    @Mock private TransactionDetailRepository transactionDetailRepository;

    @InjectMocks private LowStockServiceImpl service;

    private Site site;

    @BeforeEach
    void setup() {
        site = new Site();
        site.setSiteId(1);
        site.setSiteName("Site-1");
    }

    @Test
    void returnsEmpty_whenNoSites() {
        when(siteRepository.findAll()).thenReturn(Collections.emptyList());
        List<LowStockResponse> result = service.getLowStockDevices();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void withSerial_included_atOrBelowThreshold_and_excluded_above() {
        when(siteRepository.findAll()).thenReturn(List.of(site));
        // For with-serial: types = LAPTOP, MACBOOK, MONITOR
        // Case 1: included (available/total <= 0.04). 1/100 = 0.01 -> include
        when(deviceRepository.totalDeviceInUseAndInStock(DeviceType.LAPTOP)).thenReturn(100);
        when(deviceRepository.countAssetInStock(1, DeviceType.LAPTOP, null, null, null, null)).thenReturn(1);
        // Case 2: excluded above threshold. 10/100 = 0.10 -> exclude
        when(deviceRepository.totalDeviceInUseAndInStock(DeviceType.MACBOOK)).thenReturn(100);
        when(deviceRepository.countAssetInStock(1, DeviceType.MACBOOK, null, null, null, null)).thenReturn(10);
        // Case 3: boundary exactly equals threshold 4/100 = 0.04 -> include
        when(deviceRepository.totalDeviceInUseAndInStock(DeviceType.MONITOR)).thenReturn(100);
        when(deviceRepository.countAssetInStock(1, DeviceType.MONITOR, null, null, null, null)).thenReturn(4);

        List<LowStockResponse> result = service.getLowStockDevices();
        assertEquals(1, result.size());
        LowStockResponse resp = result.get(0);
        assertEquals(1, resp.getSiteId());
        assertEquals("Site-1", resp.getSiteName());
        // Should contain LAPTOP and MONITOR only
        assertEquals(2, resp.getLowStockTypes().size());
        assertTrue(resp.getLowStockTypes().stream().anyMatch(t -> t.getType() == DeviceType.LAPTOP && t.getTotal() == 100 && t.getAvailable() == 1));
        assertTrue(resp.getLowStockTypes().stream().anyMatch(t -> t.getType() == DeviceType.MONITOR && t.getTotal() == 100 && t.getAvailable() == 4));
        assertFalse(resp.getLowStockTypes().stream().anyMatch(t -> t.getType() == DeviceType.MACBOOK));
    }

    @Test
    void withoutSerial_included_atOrBelowThreshold_and_excluded_above() {
        when(siteRepository.findAll()).thenReturn(List.of(site));

        // Without-serial types: DONGLE, MOUSE, KEYBOARD, UBIKEY, HEADSET
        // For DONGLE: total = 100, on-site stock = 5 -> 5/100 = 0.05 -> include
        when(deviceFloorRepository.sumDeviceBySite_Type_Model(null, DeviceType.DONGLE, null)).thenReturn(20);
        when(deviceUserRepository.sumDeviceAssignedByType_Model(DeviceType.DONGLE, null)).thenReturn(30);
        when(deviceWarehouseRepository.sumStockBySite_Type_Model(null, DeviceType.DONGLE, null)).thenReturn(40);
        when(transactionDetailRepository.sumOnTheMove(DeviceType.DONGLE, null)).thenReturn(10);
        when(deviceWarehouseRepository.sumStockBySite_Type_Model(1, DeviceType.DONGLE, null)).thenReturn(5);

        // For MOUSE: total = 50, on-site stock = 3 -> 3/50 = 0.06 -> exclude
        when(deviceFloorRepository.sumDeviceBySite_Type_Model(null, DeviceType.MOUSE, null)).thenReturn(10);
        when(deviceUserRepository.sumDeviceAssignedByType_Model(DeviceType.MOUSE, null)).thenReturn(10);
        when(deviceWarehouseRepository.sumStockBySite_Type_Model(null, DeviceType.MOUSE, null)).thenReturn(20);
        when(transactionDetailRepository.sumOnTheMove(DeviceType.MOUSE, null)).thenReturn(10);
        when(deviceWarehouseRepository.sumStockBySite_Type_Model(1, DeviceType.MOUSE, null)).thenReturn(3);

        // Other types default to zero to keep simple
        when(deviceFloorRepository.sumDeviceBySite_Type_Model(null, DeviceType.KEYBOARD, null)).thenReturn(0);
        when(deviceUserRepository.sumDeviceAssignedByType_Model(DeviceType.KEYBOARD, null)).thenReturn(0);
        when(deviceWarehouseRepository.sumStockBySite_Type_Model(null, DeviceType.KEYBOARD, null)).thenReturn(0);
        when(transactionDetailRepository.sumOnTheMove(DeviceType.KEYBOARD, null)).thenReturn(0);
        when(deviceWarehouseRepository.sumStockBySite_Type_Model(1, DeviceType.KEYBOARD, null)).thenReturn(0);

        when(deviceFloorRepository.sumDeviceBySite_Type_Model(null, DeviceType.UBIKEY, null)).thenReturn(0);
        when(deviceUserRepository.sumDeviceAssignedByType_Model(DeviceType.UBIKEY, null)).thenReturn(0);
        when(deviceWarehouseRepository.sumStockBySite_Type_Model(null, DeviceType.UBIKEY, null)).thenReturn(0);
        when(transactionDetailRepository.sumOnTheMove(DeviceType.UBIKEY, null)).thenReturn(0);
        when(deviceWarehouseRepository.sumStockBySite_Type_Model(1, DeviceType.UBIKEY, null)).thenReturn(0);

        when(deviceFloorRepository.sumDeviceBySite_Type_Model(null, DeviceType.HEADSET, null)).thenReturn(0);
        when(deviceUserRepository.sumDeviceAssignedByType_Model(DeviceType.HEADSET, null)).thenReturn(0);
        when(deviceWarehouseRepository.sumStockBySite_Type_Model(null, DeviceType.HEADSET, null)).thenReturn(0);
        when(transactionDetailRepository.sumOnTheMove(DeviceType.HEADSET, null)).thenReturn(0);
        when(deviceWarehouseRepository.sumStockBySite_Type_Model(1, DeviceType.HEADSET, null)).thenReturn(0);

        List<LowStockResponse> result = service.getLowStockDevices();
        assertEquals(1, result.size());
        LowStockResponse resp = result.get(0);
        assertTrue(resp.getLowStockTypes().stream().anyMatch(t -> t.getType() == DeviceType.DONGLE && t.getTotal() == 100 && t.getAvailable() == 5));
        assertFalse(resp.getLowStockTypes().stream().anyMatch(t -> t.getType() == DeviceType.MOUSE));
    }

    @Test
    void exceptions_inside_type_loops_are_caught_and_continue() {
        when(siteRepository.findAll()).thenReturn(List.of(site));

        // Throw for a serial type and a non-serial type
        when(deviceRepository.totalDeviceInUseAndInStock(DeviceType.LAPTOP)).thenThrow(new RuntimeException("boom"));
        when(deviceFloorRepository.sumDeviceBySite_Type_Model(null, DeviceType.DONGLE, null)).thenThrow(new RuntimeException("bang"));

        // Provide values for other serial types to include one
        when(deviceRepository.totalDeviceInUseAndInStock(DeviceType.MACBOOK)).thenReturn(100);
        when(deviceRepository.countAssetInStock(1, DeviceType.MACBOOK, null, null, null, null)).thenReturn(1);
        when(deviceRepository.totalDeviceInUseAndInStock(DeviceType.MONITOR)).thenReturn(0);
        when(deviceRepository.countAssetInStock(1, DeviceType.MONITOR, null, null, null, null)).thenReturn(0);

        // Provide values for other non-serial types to include/exclude
        when(deviceFloorRepository.sumDeviceBySite_Type_Model(null, DeviceType.MOUSE, null)).thenReturn(0);
        when(deviceUserRepository.sumDeviceAssignedByType_Model(DeviceType.MOUSE, null)).thenReturn(0);
        when(deviceWarehouseRepository.sumStockBySite_Type_Model(null, DeviceType.MOUSE, null)).thenReturn(0);
        when(transactionDetailRepository.sumOnTheMove(DeviceType.MOUSE, null)).thenReturn(0);
        when(deviceWarehouseRepository.sumStockBySite_Type_Model(1, DeviceType.MOUSE, null)).thenReturn(0);

        List<LowStockResponse> result = service.getLowStockDevices();
        assertEquals(1, result.size());
        LowStockResponse resp = result.get(0);
        // Should include MACBOOK low stock despite exceptions elsewhere
        assertTrue(resp.getLowStockTypes().stream().anyMatch(t -> t.getType() == DeviceType.MACBOOK));
    }

    @Test
    void outer_exception_returns_empty_list() {
        when(siteRepository.findAll()).thenThrow(new RuntimeException("db down"));
        List<LowStockResponse> result = service.getLowStockDevices();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}


