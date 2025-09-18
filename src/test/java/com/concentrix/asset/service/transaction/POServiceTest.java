package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreatePORequest;
import com.concentrix.asset.dto.request.POItem;
import com.concentrix.asset.dto.response.POResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.POMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.DeviceService;
import com.concentrix.asset.service.impl.transaction.POServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class POServiceTest {

    @Mock PORepository poRepository;
    @Mock POMapper poMapper;
    @Mock DeviceRepository deviceRepository;
    @Mock ModelRepository modelRepository;
    @Mock DeviceWarehouseRepository deviceWarehouseRepository;
    @Mock UserRepository userRepository;
    @Mock DeviceService deviceService;

    @InjectMocks POServiceImpl poService;

    private final String currentEid = "E123";

    @BeforeEach
    void setupSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        Mockito.lenient().when(authentication.getName()).thenReturn(currentEid);
        SecurityContext securityContext = mock(SecurityContext.class);
        Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private CreatePORequest buildRequestWithItems(List<POItem> items) {
        CreatePORequest req = new CreatePORequest();
        req.setVendorId(10);
        req.setWarehouseId(20);
        req.setPoId("PO-001");
        req.setItems(items);
        req.setPoDate(LocalDate.now());
        req.setNote("note");
        return req;
    }

    private PurchaseOrder buildPOEntityWithWarehouse(Warehouse wh) {
        return PurchaseOrder.builder()
                .poId("PO-001")
                .warehouse(wh)
                .poDetails(new ArrayList<>())
                .build();
    }

    private Model buildModel(Integer id) {
        Model m = new Model();
        m.setModelId(id);
        m.setModelName("Model-" + id);
        return m;
    }

    private Warehouse buildWarehouse(Integer id) {
        Warehouse w = new Warehouse();
        w.setWarehouseId(id);
        w.setWarehouseName("WH-" + id);
        return w;
    }

    private User buildUser(String eid) {
        User u = new User();
        u.setEid(eid);
        u.setFullName("Tester");
        return u;
    }

    @Test
    @DisplayName("createPO - with serial - success path")
    void createPO_withSerial_success() {
        POItem item = POItem.builder()
                .deviceName("Laptop A")
                .modelId(1)
                .serialNumber("SN-001")
                .quantity(1)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .build();
        CreatePORequest req = buildRequestWithItems(List.of(item));

        Warehouse warehouse = buildWarehouse(20);
        PurchaseOrder poEntity = buildPOEntityWithWarehouse(warehouse);
        Model model = buildModel(1);

        when(poRepository.existsById("PO-001")).thenReturn(false);
        when(poMapper.toPurchaseOrder(req)).thenReturn(poEntity);
        when(userRepository.findById(currentEid)).thenReturn(Optional.of(buildUser(currentEid)));
        when(poRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(modelRepository.findById(1)).thenReturn(Optional.of(model));
        when(deviceRepository.findBySerialNumber("SN-001")).thenReturn(Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> {
            Device d = inv.getArgument(0);
            d.setDeviceId(100);
            return d;
        });
        when(deviceService.generateHostNameForLaptop(any(Device.class))).thenReturn("HOST-100");
        when(poMapper.toPOResponse(any(PurchaseOrder.class))).thenReturn(POResponse.builder().poId("PO-001").build());

        POResponse resp = poService.createPO(req);

        assertNotNull(resp);
        assertEquals("PO-001", resp.getPoId());
        verify(deviceRepository).findBySerialNumber("SN-001");
        verify(deviceRepository).save(any(Device.class));
        assertThat(poEntity.getPoDetails()).hasSize(1);
        PODetail detail = poEntity.getPoDetails().get(0);
        assertEquals(Integer.valueOf(1), detail.getQuantity());
        assertNotNull(detail.getDevice());
        verify(deviceWarehouseRepository, never()).findByWarehouse_WarehouseIdAndDevice_DeviceId(anyInt(), anyInt());
    }

    @Test
    @DisplayName("createPO - duplicate serials in request - throws DUPLICATE_SERIAL_NUMBER")
    void createPO_duplicateSerials_throw() {
        POItem i1 = POItem.builder().deviceName("A").modelId(1).serialNumber("SN-001").quantity(1).build();
        POItem i2 = POItem.builder().deviceName("B").modelId(2).serialNumber("SN-001").quantity(1).build();
        CreatePORequest req = buildRequestWithItems(List.of(i1, i2));

        when(poRepository.existsById("PO-001")).thenReturn(false);

        CustomException ex = assertThrows(CustomException.class, () -> poService.createPO(req));
        assertEquals(ErrorCode.DUPLICATE_SERIAL_NUMBER, ex.getErrorCode());
        verifyNoInteractions(poMapper);
    }

    @Test
    @DisplayName("createPO - PO id already exists - throws PO_ALREADY_EXISTS")
    void createPO_poAlreadyExists_throw() {
        POItem item = POItem.builder().deviceName("A").modelId(1).quantity(1).build();
        CreatePORequest req = buildRequestWithItems(List.of(item));

        when(poRepository.existsById("PO-001")).thenReturn(true);

        CustomException ex = assertThrows(CustomException.class, () -> poService.createPO(req));
        assertEquals(ErrorCode.PO_ALREADY_EXISTS, ex.getErrorCode());
        verifyNoInteractions(poMapper);
    }

    @Test
    @DisplayName("createPO - with serial - device already exists - throws DEVICE_ALREADY_EXISTS")
    void createPO_deviceAlreadyExists_throw() {
        POItem item = POItem.builder()
                .deviceName("Laptop A")
                .modelId(1)
                .serialNumber("SN-001")
                .quantity(1)
                .build();
        CreatePORequest req = buildRequestWithItems(List.of(item));

        Warehouse warehouse = buildWarehouse(20);
        PurchaseOrder poEntity = buildPOEntityWithWarehouse(warehouse);
        Model model = buildModel(1);

        when(poRepository.existsById("PO-001")).thenReturn(false);
        when(poMapper.toPurchaseOrder(req)).thenReturn(poEntity);
        when(userRepository.findById(currentEid)).thenReturn(Optional.of(buildUser(currentEid)));
        when(poRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(modelRepository.findById(1)).thenReturn(Optional.of(model));
        when(deviceRepository.findBySerialNumber("SN-001")).thenReturn(Optional.of(new Device()));

        CustomException ex = assertThrows(CustomException.class, () -> poService.createPO(req));
        assertEquals(ErrorCode.DEVICE_ALREADY_EXISTS, ex.getErrorCode());
        verify(deviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPO - without serial - existing device found - update existing DeviceWarehouse")
    void createPO_withoutSerial_existingDevice_updatesWarehouse() {
        POItem item = POItem.builder()
                .deviceName("Mouse")
                .modelId(2)
                .quantity(5)
                .build();
        CreatePORequest req = buildRequestWithItems(List.of(item));

        Warehouse wh = buildWarehouse(20);
        PurchaseOrder poEntity = buildPOEntityWithWarehouse(wh);
        Model model = buildModel(2);

        Device existingDevice = Device.builder()
                .deviceId(200)
                .deviceName("Mouse")
                .model(model)
                .status(DeviceStatus.IN_STOCK)
                .build();

        DeviceWarehouse existingDW = DeviceWarehouse.builder()
                .warehouse(wh)
                .device(existingDevice)
                .quantity(3)
                .build();

        when(poRepository.existsById("PO-001")).thenReturn(false);
        when(poMapper.toPurchaseOrder(req)).thenReturn(poEntity);
        when(userRepository.findById(currentEid)).thenReturn(Optional.of(buildUser(currentEid)));
        when(poRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(modelRepository.findById(2)).thenReturn(Optional.of(model));
        when(deviceRepository.findFirstByModel_ModelId(2)).thenReturn(Optional.of(existingDevice));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(20, 200))
                .thenReturn(Optional.of(existingDW));
        when(poMapper.toPOResponse(any(PurchaseOrder.class))).thenReturn(POResponse.builder().poId("PO-001").build());

        POResponse resp = poService.createPO(req);

        assertNotNull(resp);
        assertThat(poEntity.getPoDetails()).hasSize(1);
        assertEquals(Integer.valueOf(8), existingDW.getQuantity());
        verify(deviceWarehouseRepository).save(existingDW);
        verify(deviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPO - without serial - create new device and new DeviceWarehouse")
    void createPO_withoutSerial_createsDeviceAndWarehouse() {
        POItem item = POItem.builder()
                .deviceName("Keyboard")
                .modelId(3)
                .quantity(4)
                .build();
        CreatePORequest req = buildRequestWithItems(List.of(item));

        Warehouse wh = buildWarehouse(30);
        PurchaseOrder poEntity = buildPOEntityWithWarehouse(wh);
        Model model = buildModel(3);

        Device createdDevice = Device.builder().deviceId(300).deviceName("Keyboard").model(model).status(DeviceStatus.IN_STOCK).build();

        when(poRepository.existsById("PO-001")).thenReturn(false);
        when(poMapper.toPurchaseOrder(req)).thenReturn(poEntity);
        when(userRepository.findById(currentEid)).thenReturn(Optional.of(buildUser(currentEid)));
        when(poRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(modelRepository.findById(3)).thenReturn(Optional.of(model));
        when(deviceRepository.findFirstByModel_ModelId(3)).thenReturn(Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(createdDevice);
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(30, 300)).thenReturn(Optional.empty());
        when(poMapper.toPOResponse(any(PurchaseOrder.class))).thenReturn(POResponse.builder().poId("PO-001").build());

        POResponse resp = poService.createPO(req);

        assertNotNull(resp);
        assertThat(poEntity.getPoDetails()).hasSize(1);
        verify(deviceRepository).save(any(Device.class));
        verify(deviceWarehouseRepository).save(argThat(dw -> dw.getWarehouse().getWarehouseId().equals(30)
                && dw.getDevice().getDeviceId().equals(300)
                && dw.getQuantity().equals(4)));
    }

    @Test
    @DisplayName("createPO - model not found - throws MODEL_NOT_FOUND")
    void createPO_modelNotFound_throw() {
        POItem item = POItem.builder().deviceName("X").modelId(99).quantity(1).build();
        CreatePORequest req = buildRequestWithItems(List.of(item));

        Warehouse wh = buildWarehouse(20);
        PurchaseOrder poEntity = buildPOEntityWithWarehouse(wh);

        when(poRepository.existsById("PO-001")).thenReturn(false);
        when(poMapper.toPurchaseOrder(req)).thenReturn(poEntity);
        when(userRepository.findById(currentEid)).thenReturn(Optional.of(buildUser(currentEid)));
        when(poRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(modelRepository.findById(99)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> poService.createPO(req));
        assertEquals(ErrorCode.MODEL_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("getPOById - found returns mapped response")
    void getPOById_found() {
        PurchaseOrder po = buildPOEntityWithWarehouse(buildWarehouse(1));
        when(poRepository.findById("PO-123")).thenReturn(Optional.of(po));
        when(poMapper.toPOResponse(po)).thenReturn(POResponse.builder().poId("PO-123").build());

        POResponse resp = poService.getPOById("PO-123");

        assertEquals("PO-123", resp.getPoId());
    }

    @Test
    @DisplayName("getPOById - not found throws PO_NOT_FOUND")
    void getPOById_notFound() {
        when(poRepository.findById("PO-404")).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> poService.getPOById("PO-404"));
        assertEquals(ErrorCode.PO_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("filterPO - delegates to repo and maps results")
    void filterPO_mapsAndPaginates() {
        Pageable pageable = PageRequest.of(0, 10);
        PurchaseOrder po1 = buildPOEntityWithWarehouse(buildWarehouse(1));
        PurchaseOrder po2 = buildPOEntityWithWarehouse(buildWarehouse(2));
        Page<PurchaseOrder> page = new PageImpl<>(List.of(po1, po2), pageable, 2);

        when(poRepository.findAll(Mockito.<Specification<PurchaseOrder>>any(), eq(pageable))).thenReturn(page);
        when(poMapper.toPOResponse(po1)).thenReturn(POResponse.builder().poId("PO-1").build());
        when(poMapper.toPOResponse(po2)).thenReturn(POResponse.builder().poId("PO-2").build());

        Page<POResponse> result = poService.filterPO("po", LocalDate.now().minusDays(1), LocalDate.now(), pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals("PO-1", result.getContent().get(0).getPoId());
        assertEquals("PO-2", result.getContent().get(1).getPoId());
        verify(poRepository).findAll(Mockito.<Specification<PurchaseOrder>>any(), eq(pageable));
        verify(poMapper, times(1)).toPOResponse(po1);
        verify(poMapper, times(1)).toPOResponse(po2);
    }
}


