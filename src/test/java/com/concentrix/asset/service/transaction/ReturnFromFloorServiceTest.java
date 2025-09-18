package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateReturnFromFloorRequest;
import com.concentrix.asset.dto.request.TransactionItem;
import com.concentrix.asset.dto.response.ReturnFromFloorResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.ReturnFromFloorMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.impl.transaction.ReturnFromFloorServiceImpl;
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
class ReturnFromFloorServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private ReturnFromFloorMapper returnFromFloorMapper;
    @Mock private DeviceRepository deviceRepository;
    @Mock private DeviceWarehouseRepository deviceWarehouseRepository;
    @Mock private UserRepository userRepository;
    @Mock private DeviceFloorRepository deviceFloorRepository;

    @InjectMocks private ReturnFromFloorServiceImpl service;

    private CreateReturnFromFloorRequest request;
    private Floor fromFloor;
    private Warehouse toWarehouse;
    private AssetTransaction baseTx;
    private Device serialDeviceInFloor;
    private Device nonSerialDevice;
    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setEid("E123");

        fromFloor = new Floor();
        fromFloor.setFloorId(11);
        fromFloor.setFloorName("FL-11");

        toWarehouse = new Warehouse();
        toWarehouse.setWarehouseId(22);
        toWarehouse.setWarehouseName("WH-22");

        request = new CreateReturnFromFloorRequest();
        request.setItems(new ArrayList<>());

        baseTx = new AssetTransaction();
        baseTx.setTransactionType(TransactionType.RETURN_FROM_FLOOR);
        baseTx.setFromFloor(fromFloor);
        baseTx.setToWarehouse(toWarehouse);

        Model model = new Model();
        model.setModelId(99);
        model.setModelName("Model-99");

        serialDeviceInFloor = new Device();
        serialDeviceInFloor.setDeviceId(1);
        serialDeviceInFloor.setSerialNumber("S1");
        serialDeviceInFloor.setStatus(DeviceStatus.IN_FLOOR);
        serialDeviceInFloor.setCurrentFloor(fromFloor);
        serialDeviceInFloor.setModel(model);

        nonSerialDevice = new Device();
        nonSerialDevice.setDeviceId(2);
        nonSerialDevice.setModel(model);

        // SecurityContext lenient
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        Mockito.lenient().when(auth.getName()).thenReturn("E123");
        Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    private void setupMapper() {
        when(returnFromFloorMapper.toAssetTransaction(request)).thenReturn(baseTx);
        when(userRepository.findById("E123")).thenReturn(Optional.of(currentUser));
    }

    @Test
    void getById_success() {
        AssetTransaction tx = new AssetTransaction();
        when(transactionRepository.findById(1)).thenReturn(Optional.of(tx));
        when(returnFromFloorMapper.toReturnFromFloorResponse(tx)).thenReturn(new ReturnFromFloorResponse());

        ReturnFromFloorResponse resp = service.getReturnFromFloorById(1);
        assertNotNull(resp);
    }

    @Test
    void getById_notFound() {
        when(transactionRepository.findById(404)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.getReturnFromFloorById(404));
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createReturn_duplicateSerial_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        setupMapper();

        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromFloor(request));
        assertEquals(ErrorCode.DUPLICATE_SERIAL_NUMBER, ex.getErrorCode());
    }

    @Test
    void createReturn_duplicateModel_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        setupMapper();

        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromFloor(request));
        assertEquals(ErrorCode.DUPLICATE_SERIAL_NUMBER, ex.getErrorCode());
    }

    @Test
    void createReturn_serialNotFound_collectsAndThrows() {
        request.getItems().add(TransactionItem.builder().serialNumber("S404").modelId(null).quantity(1).build());
        setupMapper();
        when(deviceRepository.findBySerialNumber("S404")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromFloor(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("S404"));
    }

    @Test
    void createReturn_serialInvalidStatus_collectsAndThrows() {
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        setupMapper();
        serialDeviceInFloor.setStatus(DeviceStatus.IN_STOCK);
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDeviceInFloor));

        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromFloor(request));
        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, ex.getErrorCode());
    }

    @Test
    void createReturn_serialWrongFloor_collectsAndThrows() {
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        setupMapper();
        Floor other = new Floor(); other.setFloorId(777);
        serialDeviceInFloor.setCurrentFloor(other);
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDeviceInFloor));

        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromFloor(request));
        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, ex.getErrorCode());
    }

    @Test
    void createReturn_modelNotFound_aggregated() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        setupMapper();
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromFloor(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Model ID: 99"));
    }

    @Test
    void createReturn_success_serial_updatesDevice() {
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        setupMapper();
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDeviceInFloor));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(returnFromFloorMapper.toReturnFromFloorResponse(any(AssetTransaction.class)))
                .thenReturn(new ReturnFromFloorResponse());

        ReturnFromFloorResponse resp = service.createReturnFromFloor(request);

        assertNotNull(resp);
        verify(deviceRepository).save(any(Device.class));
        verify(transactionRepository).save(any(AssetTransaction.class));
    }

    @Test
    void createReturn_nonSerial_missingDeviceFloor_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(2).build());
        setupMapper();
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        when(deviceFloorRepository.findByDevice_DeviceIdAndFloor_FloorId(2, 11)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromFloor(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND_IN_FLOOR, ex.getErrorCode());
    }

    @Test
    void createReturn_nonSerial_insufficientFloorQty_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(5).build());
        setupMapper();
        DeviceFloor df = new DeviceFloor(); df.setQuantity(3);
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        when(deviceFloorRepository.findByDevice_DeviceIdAndFloor_FloorId(2, 11)).thenReturn(Optional.of(df));

        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromFloor(request));
        assertEquals(ErrorCode.DEVICE_NOT_ENOUGH_IN_FLOOR, ex.getErrorCode());
    }

    @Test
    void createReturn_success_nonSerial_updatesFloorAndWarehouse() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(2).build());
        setupMapper();
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        DeviceFloor df = new DeviceFloor(); df.setQuantity(5);
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        when(deviceFloorRepository.findByDevice_DeviceIdAndFloor_FloorId(2, 11)).thenReturn(Optional.of(df));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(22, 2))
                .thenReturn(Optional.empty());
        when(returnFromFloorMapper.toReturnFromFloorResponse(any(AssetTransaction.class)))
                .thenReturn(new ReturnFromFloorResponse());

        ReturnFromFloorResponse resp = service.createReturnFromFloor(request);

        assertNotNull(resp);
        assertEquals(3, df.getQuantity());
        verify(deviceFloorRepository, atLeastOnce()).save(df);
        verify(deviceWarehouseRepository, atLeastOnce()).save(any(DeviceWarehouse.class));
    }

    @Test
    void filter_invalidDateRange_throws() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate from = LocalDate.now();
        LocalDate to = from.minusDays(1);
        CustomException ex = assertThrows(CustomException.class,
                () -> service.filterReturnFromFloors(null, from, to, pageable));
        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
    }

    @Test
    void filter_withFilters_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AssetTransaction> txPage = new PageImpl<>(Collections.singletonList(new AssetTransaction()), pageable, 1);
        when(transactionRepository.findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable))).thenReturn(txPage);
        when(returnFromFloorMapper.toReturnFromFloorResponse(any(AssetTransaction.class)))
                .thenReturn(new ReturnFromFloorResponse());

        Page<ReturnFromFloorResponse> page = service.filterReturnFromFloors("abc", LocalDate.now().minusDays(7), LocalDate.now(), pageable);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable));
    }

    @Test
    void filter_noFilters_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AssetTransaction> txPage = new PageImpl<>(Collections.singletonList(new AssetTransaction()), pageable, 1);
        when(transactionRepository.findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable))).thenReturn(txPage);
        when(returnFromFloorMapper.toReturnFromFloorResponse(any(AssetTransaction.class)))
                .thenReturn(new ReturnFromFloorResponse());

        Page<ReturnFromFloorResponse> page = service.filterReturnFromFloors(null, null, null, pageable);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
    }
}


