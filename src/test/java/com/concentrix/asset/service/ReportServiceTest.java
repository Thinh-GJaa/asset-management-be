package com.concentrix.asset.service;

import com.concentrix.asset.dto.response.*;
import com.concentrix.asset.entity.Device;
import com.concentrix.asset.entity.Floor;
import com.concentrix.asset.entity.Model;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.entity.Warehouse;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.mapper.DeviceMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private SiteRepository siteRepository;
    @Mock private FloorRepository floorRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private ModelRepository modelRepository;
    @Mock private DeviceWarehouseRepository deviceWarehouseRepository;
    @Mock private TransactionDetailRepository transactionDetailRepository;
    @Mock private DeviceMapper deviceMapper;
    @Mock private TypeService typeService;
    @Mock private DeviceFloorRepository deviceFloorRepository;
    @Mock private DeviceUserRepository deviceUserRepository;

    @InjectMocks private ReportServiceImpl service;

    @Test
    void getStatusSummaryAllSite_coversAllStatuses() {
        when(deviceRepository.countByStatusAndSerialNumberIsNotNull(any())).thenReturn(10);
        when(deviceWarehouseRepository.sumAllStock()).thenReturn(5);
        when(deviceFloorRepository.sumDeviceInFloor()).thenReturn(6);
        when(transactionDetailRepository.sumAllOnTheMove()).thenReturn(7);
        when(transactionDetailRepository.sumAllDisposal()).thenReturn(3);
        when(transactionDetailRepository.sumAllEWaste()).thenReturn(2);
        when(transactionDetailRepository.sumAllRepair()).thenReturn(9);
        when(transactionDetailRepository.sumAllReturnFromRepair()).thenReturn(4);
        when(deviceUserRepository.sumDeviceAssigned()).thenReturn(8);

        Map<String, Map<String, Integer>> map = service.getStatusSummaryAllSite();
        assertEquals(DeviceStatus.values().length, map.size());
        assertEquals(10, map.get(DeviceStatus.IN_STOCK.name()).get("withSerial"));
        assertEquals(5, map.get(DeviceStatus.IN_STOCK.name()).get("withoutSerial"));
        assertEquals(6, map.get(DeviceStatus.IN_FLOOR.name()).get("withoutSerial"));
        assertEquals(7, map.get(DeviceStatus.ON_THE_MOVE.name()).get("withoutSerial"));
        assertEquals(3, map.get(DeviceStatus.DISPOSED.name()).get("withoutSerial"));
        assertEquals(2, map.get(DeviceStatus.E_WASTE.name()).get("withoutSerial"));
        assertEquals(9 - 4, map.get(DeviceStatus.REPAIR.name()).get("withoutSerial"));
        assertEquals(8, map.get(DeviceStatus.ASSIGNED.name()).get("withoutSerial"));
    }

    @Test
    void getWithoutSerialSummary_siteStatuses_and_globalStatuses() {
        // Site-specific statuses
        Site site = new Site(); site.setSiteId(1); site.setSiteName("S1");
        when(siteRepository.findAll()).thenReturn(List.of(site));
        Model model = new Model(); model.setModelId(11); model.setModelName("M1"); model.setType(DeviceType.MOUSE);
        when(modelRepository.findByType(DeviceType.MOUSE)).thenReturn(List.of(model));
        // IN_STOCK path
        when(deviceWarehouseRepository.sumStockBySite_Type_Model(1, DeviceType.MOUSE, 11)).thenReturn(3);
        List<SiteDeviceWithoutSerialSummaryResponse> stock = service.getWithoutSerialSummary(DeviceStatus.IN_STOCK, DeviceType.MOUSE, null);
        assertEquals(1, stock.size());
        assertEquals(1, stock.get(0).getTypes().size());
        assertEquals(3, stock.get(0).getTypes().get(0).getTotal());

        // IN_FLOOR path
        when(deviceFloorRepository.sumDeviceBySite_Type_Model(1, DeviceType.MOUSE, 11)).thenReturn(2);
        List<SiteDeviceWithoutSerialSummaryResponse> floor = service.getWithoutSerialSummary(DeviceStatus.IN_FLOOR, DeviceType.MOUSE, null);
        assertEquals(2, floor.get(0).getTypes().get(0).getTotal());

        // E_WASTE path
        when(transactionDetailRepository.sumEWasteBySite_Type_Model(1, DeviceType.MOUSE, 11)).thenReturn(4);
        List<SiteDeviceWithoutSerialSummaryResponse> ew = service.getWithoutSerialSummary(DeviceStatus.E_WASTE, DeviceType.MOUSE, null);
        assertEquals(4, ew.get(0).getTypes().get(0).getTotal());

        // Global statuses (no site grouping)
        when(modelRepository.findByType(DeviceType.DONGLE)).thenReturn(List.of(model));
        when(deviceUserRepository.sumDeviceAssignedByType_Model(DeviceType.DONGLE, 11)).thenReturn(5);
        List<SiteDeviceWithoutSerialSummaryResponse> assigned = service.getWithoutSerialSummary(DeviceStatus.ASSIGNED, DeviceType.DONGLE, null);
        assertEquals(1, assigned.size());
        assertEquals("All Sites", assigned.get(0).getSiteName());
        assertEquals(5, assigned.get(0).getTypes().get(0).getTotal());

        when(transactionDetailRepository.sumOnTheMove(DeviceType.DONGLE, 11)).thenReturn(6);
        List<SiteDeviceWithoutSerialSummaryResponse> onMove = service.getWithoutSerialSummary(DeviceStatus.ON_THE_MOVE, DeviceType.DONGLE, null);
        assertEquals(6, onMove.get(0).getTypes().get(0).getTotal());

        when(transactionDetailRepository.sumDisposedByType_Model(DeviceType.DONGLE, 11)).thenReturn(7);
        List<SiteDeviceWithoutSerialSummaryResponse> disposed = service.getWithoutSerialSummary(DeviceStatus.DISPOSED, DeviceType.DONGLE, null);
        assertEquals(7, disposed.get(0).getTypes().get(0).getTotal());

        when(transactionDetailRepository.sumRepair(DeviceType.DONGLE, 11)).thenReturn(9);
        when(transactionDetailRepository.sumReturnFromRepair(DeviceType.DONGLE, 11)).thenReturn(4);
        List<SiteDeviceWithoutSerialSummaryResponse> repair = service.getWithoutSerialSummary(DeviceStatus.REPAIR, DeviceType.DONGLE, null);
        assertEquals(5, repair.get(0).getTypes().get(0).getTotal());
    }

    @Test
    void getWithSerialSummary_siteStatuses_and_defaultStatuses() {
        // Setup data
        Site site = new Site(); site.setSiteId(1); site.setSiteName("S1");
        when(siteRepository.findAll()).thenReturn(List.of(site));
        Model model = new Model(); model.setModelId(100); model.setModelName("Model"); model.setType(DeviceType.LAPTOP);
        when(modelRepository.findByType(DeviceType.LAPTOP)).thenReturn(List.of(model));

        // IN_STOCK with site summaries present
        when(deviceRepository.countAssetInStock(eq(1), eq(DeviceType.LAPTOP), eq(100), any(), any(), any())).thenReturn(3);
        List<TypeSummaryResponse> inStock = service.getWithSerialSummary(null, DeviceStatus.IN_STOCK, null, null, null, DeviceType.LAPTOP, 100, null, null);
        assertEquals(1, inStock.size());
        assertEquals(3, inStock.get(0).getModels().get(0).getTotal());
        assertNotNull(inStock.get(0).getModels().get(0).getSites());

        // IN_FLOOR with site summaries present
        when(deviceRepository.countAssetInFloor(eq(1), any(), any(), any(), eq(DeviceType.LAPTOP), eq(100), any(), any(), any())).thenReturn(2);
        List<TypeSummaryResponse> inFloor = service.getWithSerialSummary(null, DeviceStatus.IN_FLOOR, 1, 2, 3, DeviceType.LAPTOP, 100, null, null);
        assertEquals(2, inFloor.get(0).getModels().get(0).getTotal());

        // E_WASTE with site summaries present
        when(deviceRepository.countAssetEWaste(eq(1), eq(DeviceType.LAPTOP), eq(100), any(), any(), any())).thenReturn(4);
        List<TypeSummaryResponse> ew = service.getWithSerialSummary(null, DeviceStatus.E_WASTE, null, null, null, DeviceType.LAPTOP, 100, null, null);
        assertEquals(4, ew.get(0).getModels().get(0).getTotal());

        // Default statuses aggregated (no site summaries)
        when(deviceRepository.countAssetByStatus(DeviceStatus.DISPOSED, DeviceType.LAPTOP, 100, null, null, null)).thenReturn(7);
        List<TypeSummaryResponse> disp = service.getWithSerialSummary(null, DeviceStatus.DISPOSED, null, null, null, DeviceType.LAPTOP, 100, null, null);
        assertEquals(7, disp.get(0).getModels().get(0).getTotal());
        assertNull(disp.get(0).getModels().get(0).getSites());
    }

    @Test
    void getDeviceListForReport_switches_and_mapping() {
        // Prepare devices and mapping
        Device d1 = new Device(); d1.setDeviceId(1);
        Device d2 = new Device(); d2.setDeviceId(2);
        DeviceResponse r1 = new DeviceResponse(); r1.setDeviceId(1);
        DeviceResponse r2 = new DeviceResponse(); r2.setDeviceId(2);
        when(deviceMapper.toDeviceResponse(d1)).thenReturn(r1);
        when(deviceMapper.toDeviceResponse(d2)).thenReturn(r2);

        // IN_STOCK
        when(deviceRepository.findDevicesInStockForReport(any(), any(), any(), any(), any(), any())).thenReturn(List.of(d1));
        List<DeviceResponse> s1 = service.getDeviceListForReport(1, DeviceStatus.IN_STOCK, null, null, null, DeviceType.LAPTOP, 100, null, null);
        assertEquals(1, s1.size());

        // IN_FLOOR
        when(deviceRepository.findDevicesInFloorForReport(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(d2));
        List<DeviceResponse> s2 = service.getDeviceListForReport(1, DeviceStatus.IN_FLOOR, 1, 2, 3, DeviceType.LAPTOP, 100, null, null);
        assertEquals(1, s2.size());

        // E_WASTE
        when(deviceRepository.findDevicesEWasteForReport(any(), any(), any(), any(), any(), any())).thenReturn(List.of(d1, d2));
        List<DeviceResponse> s3 = service.getDeviceListForReport(1, DeviceStatus.E_WASTE, null, null, null, DeviceType.LAPTOP, 100, null, null);
        assertEquals(2, s3.size());

        // Default
        when(deviceRepository.findDevicesStatusForReport(any(), any(), any(), any(), any(), any())).thenReturn(List.of(d1));
        List<DeviceResponse> s4 = service.getDeviceListForReport(1, DeviceStatus.DISPOSED, null, null, null, DeviceType.LAPTOP, 100, null, null);
        assertEquals(1, s4.size());
    }

    @Test
    void getSiteTypeChartWithSerial_siteAndDefault() {
        Site site = new Site(); site.setSiteId(1); site.setSiteName("S1");
        when(siteRepository.findAll()).thenReturn(List.of(site));

        // Build devices across types and statuses
        Model mLaptop = new Model(); mLaptop.setType(DeviceType.LAPTOP);
        Model mMouse = new Model(); mMouse.setType(DeviceType.MOUSE);
        Device ds1 = new Device(); ds1.setModel(mLaptop); ds1.setStatus(DeviceStatus.IN_STOCK); Warehouse w = new Warehouse(); Site ws = new Site(); ws.setSiteId(1); w.setSite(ws); ds1.setCurrentWarehouse(w);
        Device ds2 = new Device(); ds2.setModel(mMouse); ds2.setStatus(DeviceStatus.IN_STOCK); ds2.setCurrentWarehouse(w);
        Device df1 = new Device(); df1.setModel(mLaptop); df1.setStatus(DeviceStatus.IN_STOCK); Floor f = new Floor(); f.setSite(ws); df1.setCurrentFloor(f);
        when(deviceRepository.findAllBySerialNumberIsNotNull()).thenReturn(List.of(ds1, ds2, df1));

        // Site-specific status
        List<SiteTypeChartResponse> siteResp = service.getSiteTypeChartWithSerial(DeviceStatus.IN_STOCK);
        assertEquals(1, siteResp.size());
        assertEquals("S1", siteResp.get(0).getSiteName());
        assertTrue(siteResp.get(0).getTypeCounts().stream().anyMatch(tc -> tc.getType().equals(DeviceType.LAPTOP.name())));
        assertTrue(siteResp.get(0).getTypeCounts().stream().anyMatch(tc -> tc.getType().equals("OTHER")));

        // Default status -> aggregated
        when(deviceRepository.findAllBySerialNumberIsNotNull()).thenReturn(List.of(ds1));
        List<SiteTypeChartResponse> defResp = service.getSiteTypeChartWithSerial(DeviceStatus.DISPOSED);
        assertEquals(1, defResp.size());
        assertEquals("All Sites", defResp.get(0).getSiteName());
    }

    @Test
    void getSiteTypeChartWithoutSerial_globalAndPerSite() {
        when(typeService.getTypeWithoutSerial()).thenReturn(List.of(DeviceType.MOUSE, DeviceType.KEYBOARD));
        Site site = new Site(); site.setSiteId(1); site.setSiteName("S1");
        when(siteRepository.findAll()).thenReturn(List.of(site));

        // Global status (DISPOSED) - respond for any type with modelId null
        when(transactionDetailRepository.sumDisposedByType_Model(any(), isNull())).thenReturn(0);
        List<SiteTypeChartResponse> global = service.getSiteTypeChartWithoutSerial(DeviceStatus.DISPOSED);
        assertEquals(1, global.size());
        assertEquals("All Sites", global.get(0).getSiteName());

        // Per-site status IN_STOCK
        when(deviceWarehouseRepository.sumStockBySite_Type_Model(eq(1), any(DeviceType.class), isNull()))
            .thenAnswer(inv -> ((DeviceType) inv.getArgument(1)) == DeviceType.MOUSE ? 5 : 0);
        List<SiteTypeChartResponse> perSite = service.getSiteTypeChartWithoutSerial(DeviceStatus.IN_STOCK);
        assertEquals(1, perSite.size());
        assertEquals("S1", perSite.get(0).getSiteName());
        assertTrue(perSite.get(0).getTypeCounts().stream().anyMatch(tc -> tc.getType().equals(DeviceType.MOUSE.name())));
    }

    @Test
    void getDateRangeFromAgeRange_allBuckets() {
        LocalDate[] le1 = service.getDateRangeFromAgeRange("<=1");
        assertNotNull(le1[0]);
        LocalDate[] y12 = service.getDateRangeFromAgeRange("1-2");
        assertNotNull(y12[0]); assertNotNull(y12[1]);
        LocalDate[] y23 = service.getDateRangeFromAgeRange("2-3");
        assertNotNull(y23[0]); assertNotNull(y23[1]);
        LocalDate[] y34 = service.getDateRangeFromAgeRange("3-4");
        assertNotNull(y34[0]); assertNotNull(y34[1]);
        LocalDate[] y45 = service.getDateRangeFromAgeRange("4-5");
        assertNotNull(y45[0]); assertNotNull(y45[1]);
        LocalDate[] y56 = service.getDateRangeFromAgeRange("5-6");
        assertNotNull(y56[0]); assertNotNull(y56[1]);
        LocalDate[] gt6 = service.getDateRangeFromAgeRange(">6");
        assertNull(gt6[0]); assertNotNull(gt6[1]);
        LocalDate[] nullRange = service.getDateRangeFromAgeRange(null);
        assertNull(nullRange[0]); assertNull(nullRange[1]);
    }
}


