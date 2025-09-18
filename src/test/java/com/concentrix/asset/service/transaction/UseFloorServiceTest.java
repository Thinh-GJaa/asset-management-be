package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateUseFloorRequest;
import com.concentrix.asset.dto.request.TransactionItem;
import com.concentrix.asset.dto.response.UseFloorResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.UseFloorMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.DeviceService;
import com.concentrix.asset.service.impl.transaction.UseFloorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UseFloorServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private UseFloorMapper mapper;
    @Mock private DeviceRepository deviceRepository;
    @Mock private DeviceWarehouseRepository deviceWarehouseRepository;
    @Mock private FloorRepository floorRepository;
    @Mock private UserRepository userRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private DeviceService deviceService;
    @Mock private DeviceFloorRepository deviceFloorRepository;

    @InjectMocks private UseFloorServiceImpl service;

    private CreateUseFloorRequest request;
    private Floor toFloor;
    private Warehouse fromWarehouse;
    private AssetTransaction baseTx;
    private User currentUser;
    private Device serialDevice;
    private Device nonSerialDevice;

    @BeforeEach
    void setup() {
        currentUser = new User(); currentUser.setEid("E1");

        Site site = new Site(); site.setSiteId(100);
        toFloor = new Floor(); toFloor.setFloorId(2); toFloor.setFloorName("F2"); toFloor.setSite(site);
        fromWarehouse = new Warehouse(); fromWarehouse.setWarehouseId(1); fromWarehouse.setWarehouseName("W1"); fromWarehouse.setSite(site);

        request = new CreateUseFloorRequest();
        request.setFromWarehouseId(1);
        request.setToFloorId(2);
        request.setItems(new ArrayList<>());

        baseTx = new AssetTransaction();
        baseTx.setTransactionType(TransactionType.USE_FLOOR);
        baseTx.setFromWarehouse(fromWarehouse);
        baseTx.setToFloor(toFloor);

        Model model = new Model(); model.setModelId(99); model.setModelName("Model-99");

        serialDevice = new Device();
        serialDevice.setDeviceId(10);
        serialDevice.setSerialNumber("S1");
        serialDevice.setStatus(DeviceStatus.IN_STOCK);
        serialDevice.setCurrentWarehouse(fromWarehouse);
        serialDevice.setModel(model);

        nonSerialDevice = new Device();
        nonSerialDevice.setDeviceId(20);
        nonSerialDevice.setModel(model);

        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        Mockito.lenient().when(auth.getName()).thenReturn("E1");
        Mockito.lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private void mapBase() {
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(fromWarehouse));
        when(floorRepository.findById(2)).thenReturn(Optional.of(toFloor));
        when(mapper.toAssetTransaction(request)).thenReturn(baseTx);
        when(userRepository.findById("E1")).thenReturn(Optional.of(currentUser));
    }

    @Test
    void getById_success_and_notFound() {
        AssetTransaction tx = new AssetTransaction();
        when(transactionRepository.findById(1)).thenReturn(Optional.of(tx));
        when(mapper.toUseFloorResponse(tx)).thenReturn(new UseFloorResponse());
        assertNotNull(service.getUseFloorById(1));
        when(transactionRepository.findById(404)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.getUseFloorById(404));
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void create_invalid_site_mismatch() {
        // toFloor site != fromWarehouse site
        Site other = new Site(); other.setSiteId(200);
        Floor wrongFloor = new Floor(); wrongFloor.setFloorId(2); wrongFloor.setSite(other);
        when(floorRepository.findById(2)).thenReturn(Optional.of(wrongFloor));
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(fromWarehouse));
        when(mapper.toAssetTransaction(request)).thenReturn(baseTx);
        when(userRepository.findById("E1")).thenReturn(Optional.of(currentUser));
        CustomException ex = assertThrows(CustomException.class, () -> service.createUseFloor(request));
        assertEquals(ErrorCode.INVALID_USE_FLOOR, ex.getErrorCode());
    }

    @Test
    void create_duplicates_serial_or_model() {
        mapBase();
        request.getItems().add(TransactionItem.builder().serialNumber("S1").quantity(1).build());
        request.getItems().add(TransactionItem.builder().serialNumber("S1").quantity(1).build());
        CustomException ex1 = assertThrows(CustomException.class, () -> service.createUseFloor(request));
        assertEquals(ErrorCode.DUPLICATE_SERIAL_NUMBER, ex1.getErrorCode());

        request.setItems(new ArrayList<>());
        request.getItems().add(TransactionItem.builder().modelId(99).quantity(1).build());
        request.getItems().add(TransactionItem.builder().modelId(99).quantity(1).build());
        CustomException ex2 = assertThrows(CustomException.class, () -> service.createUseFloor(request));
        assertEquals(ErrorCode.DUPLICATE_SERIAL_NUMBER, ex2.getErrorCode());
    }

    @Test
    void create_serial_notFound_invalid_or_wrongWarehouse() {
        mapBase();
        // serial not found aggregated
        request.getItems().add(TransactionItem.builder().serialNumber("S404").quantity(1).build());
        request.getItems().add(TransactionItem.builder().serialNumber("S405").quantity(1).build());
        when(deviceRepository.findBySerialNumber("S404")).thenReturn(Optional.empty());
        when(deviceRepository.findBySerialNumber("S405")).thenReturn(Optional.empty());
        CustomException nf = assertThrows(CustomException.class, () -> service.createUseFloor(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, nf.getErrorCode());

        // invalid status or wrong warehouse
        request.setItems(new ArrayList<>());
        request.getItems().add(TransactionItem.builder().serialNumber("S1").quantity(1).build());
        // wrong status
        serialDevice.setStatus(DeviceStatus.REPAIR);
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        CustomException inv = assertThrows(CustomException.class, () -> service.createUseFloor(request));
        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, inv.getErrorCode());

        // right status but wrong warehouse
        request.setItems(new ArrayList<>());
        request.getItems().add(TransactionItem.builder().serialNumber("S1").quantity(1).build());
        serialDevice.setStatus(DeviceStatus.IN_STOCK);
        Warehouse otherW = new Warehouse(); otherW.setWarehouseId(999);
        serialDevice.setCurrentWarehouse(otherW);
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        CustomException wrongWh = assertThrows(CustomException.class, () -> service.createUseFloor(request));
        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, wrongWh.getErrorCode());
    }

    @Test
    void create_model_notFound_and_missingIds() {
        mapBase();
        // model not found
        request.getItems().add(TransactionItem.builder().modelId(99).quantity(1).build());
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.empty());
        CustomException ex1 = assertThrows(CustomException.class, () -> service.createUseFloor(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex1.getErrorCode());

        // missing ids
        request.setItems(new ArrayList<>());
        request.getItems().add(TransactionItem.builder().quantity(1).build());
        CustomException ex2 = assertThrows(CustomException.class, () -> service.createUseFloor(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex2.getErrorCode());
    }

    @Test
    void create_nonSerial_missing_or_insufficient_stock() {
        mapBase();
        request.getItems().add(TransactionItem.builder().modelId(99).quantity(5).build());
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));

        // missing from warehouse stock
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(1, 20)).thenReturn(Optional.empty());
        CustomException ex1 = assertThrows(CustomException.class, () -> service.createUseFloor(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE, ex1.getErrorCode());

        // insufficient stock
        DeviceWarehouse fromStock = new DeviceWarehouse(); fromStock.setQuantity(3);
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(1, 20)).thenReturn(Optional.of(fromStock));
        CustomException ex2 = assertThrows(CustomException.class, () -> service.createUseFloor(request));
        assertEquals(ErrorCode.STOCK_OUT, ex2.getErrorCode());
    }

    @Test
    void create_success_serial_updates_device_and_hostname() {
        mapBase();
        request.getItems().add(TransactionItem.builder().serialNumber("S1").quantity(1).build());
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        when(deviceService.generateHostNameForDesktop(eq(serialDevice), any(Floor.class))).thenReturn("HOST-F2");
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toUseFloorResponse(any(AssetTransaction.class))).thenReturn(new UseFloorResponse());

        UseFloorResponse resp = service.createUseFloor(request);
        assertNotNull(resp);
        assertEquals(DeviceStatus.IN_FLOOR, serialDevice.getStatus());
        assertEquals(toFloor, serialDevice.getCurrentFloor());
        verify(deviceRepository).save(serialDevice);
    }

    @Test
    void create_success_nonSerial_updates_warehouse_and_floor() {
        mapBase();
        request.getItems().add(TransactionItem.builder().modelId(99).quantity(2).build());
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        DeviceWarehouse fromStock = new DeviceWarehouse(); fromStock.setQuantity(5);
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(1, 20)).thenReturn(Optional.of(fromStock));
        DeviceFloor deviceFloor = new DeviceFloor(); deviceFloor.setQuantity(1); deviceFloor.setFloor(toFloor);
        when(deviceFloorRepository.findByDevice_DeviceIdAndFloor_FloorId(20, 2)).thenReturn(Optional.of(deviceFloor));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toUseFloorResponse(any(AssetTransaction.class))).thenReturn(new UseFloorResponse());

        UseFloorResponse resp = service.createUseFloor(request);
        assertNotNull(resp);
        assertEquals(3, fromStock.getQuantity());
        assertEquals(3, deviceFloor.getQuantity());
        verify(deviceWarehouseRepository).save(fromStock);
        verify(deviceFloorRepository).save(deviceFloor);
    }

    @Test
    void filter_cases() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate from = LocalDate.now(); LocalDate to = from.minusDays(1);
        CustomException ex = assertThrows(CustomException.class, () -> service.filterUseFloors(null, from, to, pageable));
        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());

        Page<AssetTransaction> txPage = new PageImpl<>(Collections.singletonList(new AssetTransaction()), pageable, 1);
        when(transactionRepository.findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable))).thenReturn(txPage);
        when(mapper.toUseFloorResponse(any(AssetTransaction.class))).thenReturn(new UseFloorResponse());
        Page<UseFloorResponse> page = service.filterUseFloors("q", LocalDate.now().minusDays(7), LocalDate.now(), pageable);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable));
    }
}


