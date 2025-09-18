package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateRepairRequest;
import com.concentrix.asset.dto.request.TransactionItem;
import com.concentrix.asset.dto.response.RepairResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.RepairMapper;
import com.concentrix.asset.repository.DeviceRepository;
import com.concentrix.asset.repository.DeviceWarehouseRepository;
import com.concentrix.asset.repository.TransactionRepository;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.service.impl.transaction.RepairServiceImpl;
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
class RepairServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private RepairMapper repairMapper;
    @Mock private UserRepository userRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private DeviceWarehouseRepository deviceWarehouseRepository;

    @InjectMocks private RepairServiceImpl repairService;

    private CreateRepairRequest request;
    private Warehouse fromWarehouse;
    private Warehouse toWarehouse;
    private AssetTransaction baseTransaction;
    private Device serialDevice;
    private Device nonSerialDevice;
    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setEid("E111");
        currentUser.setFullName("Test User");

        fromWarehouse = new Warehouse();
        fromWarehouse.setWarehouseId(10);
        fromWarehouse.setWarehouseName("WH-10");

        toWarehouse = new Warehouse();
        toWarehouse.setWarehouseId(20);
        toWarehouse.setWarehouseName("WH-20");

        request = new CreateRepairRequest();
        request.setItems(new ArrayList<>());

        baseTransaction = new AssetTransaction();
        baseTransaction.setTransactionType(TransactionType.REPAIR);
        baseTransaction.setFromWarehouse(fromWarehouse);
        baseTransaction.setToWarehouse(toWarehouse);

        Model model = new Model();
        model.setModelId(99);
        model.setModelName("Model-99");

        serialDevice = new Device();
        serialDevice.setDeviceId(1);
        serialDevice.setSerialNumber("S1");
        serialDevice.setStatus(DeviceStatus.IN_STOCK);
        serialDevice.setCurrentWarehouse(fromWarehouse);
        serialDevice.setModel(model);

        nonSerialDevice = new Device();
        nonSerialDevice.setDeviceId(2);
        nonSerialDevice.setModel(model);

        // SecurityContext
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        Mockito.lenient().when(auth.getName()).thenReturn("E111");
        Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    private void setupMapperToBaseTransaction() {
        when(repairMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(userRepository.findById("E111")).thenReturn(Optional.of(currentUser));
    }

    @Test
    void getRepairById_success() {
        AssetTransaction tx = new AssetTransaction();
        when(transactionRepository.findById(1)).thenReturn(Optional.of(tx));
        when(repairMapper.toRepairResponse(tx)).thenReturn(new RepairResponse());

        RepairResponse resp = repairService.getRepairById(1);
        assertNotNull(resp);
        verify(transactionRepository).findById(1);
        verify(repairMapper).toRepairResponse(tx);
    }

    @Test
    void getRepairById_notFound_throws() {
        when(transactionRepository.findById(999)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> repairService.getRepairById(999));
        assertEquals(ErrorCode.REPAIR_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createRepair_duplicateSerial_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        setupMapperToBaseTransaction();

        CustomException ex = assertThrows(CustomException.class, () -> repairService.createRepair(request));
        assertEquals(ErrorCode.DUPLICATE_SERIAL_NUMBER, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("S1"));
    }

    @Test
    void createRepair_duplicateModel_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        setupMapperToBaseTransaction();

        CustomException ex = assertThrows(CustomException.class, () -> repairService.createRepair(request));
        assertEquals(ErrorCode.DUPLICATE_SERIAL_NUMBER, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Model ID: 99"));
    }

    @Test
    void createRepair_serialNotFound_collectsAndThrows() {
        request.getItems().add(TransactionItem.builder().serialNumber("S404").modelId(null).quantity(1).build());
        setupMapperToBaseTransaction();
        when(deviceRepository.findBySerialNumber("S404")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> repairService.createRepair(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("S404"));
    }

    @Test
    void createRepair_modelNotFound_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        setupMapperToBaseTransaction();
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> repairService.createRepair(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Model ID: 99"));
    }

    @Test
    void createRepair_missingSerialAndModel_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(null).quantity(1).build());
        setupMapperToBaseTransaction();

        CustomException ex = assertThrows(CustomException.class, () -> repairService.createRepair(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Either serialNumber or modelId"));
    }

    @Test
    void createRepair_serialInvalidStatus_collectsAndThrows() {
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        setupMapperToBaseTransaction();
        serialDevice.setStatus(DeviceStatus.IN_FLOOR);
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));

        CustomException ex = assertThrows(CustomException.class, () -> repairService.createRepair(request));
        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("S1"));
    }

    @Test
    void createRepair_serialWrongWarehouse_collectsAndThrows() {
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        setupMapperToBaseTransaction();
        Warehouse other = new Warehouse();
        other.setWarehouseId(999);
        serialDevice.setCurrentWarehouse(other);
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));

        CustomException ex = assertThrows(CustomException.class, () -> repairService.createRepair(request));
        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("S1"));
    }

    @Test
    void createRepair_nonSerial_missingStock_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(2).build());
        setupMapperToBaseTransaction();
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(10, 2))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> repairService.createRepair(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE, ex.getErrorCode());
    }

    @Test
    void createRepair_nonSerial_insufficientStock_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(5).build());
        setupMapperToBaseTransaction();
        DeviceWarehouse dw = new DeviceWarehouse();
        dw.setQuantity(3);
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(10, 2))
                .thenReturn(Optional.of(dw));

        CustomException ex = assertThrows(CustomException.class, () -> repairService.createRepair(request));
        assertEquals(ErrorCode.STOCK_OUT, ex.getErrorCode());
    }

    @Test
    void createRepair_success_serial_updatesDeviceAndSaves() {
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        setupMapperToBaseTransaction();
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        when(repairMapper.toRepairResponse(any(AssetTransaction.class))).thenReturn(new RepairResponse());

        RepairResponse resp = repairService.createRepair(request);

        assertNotNull(resp);
        verify(transactionRepository).save(any(AssetTransaction.class));
        verify(deviceRepository).save(any(Device.class));
        verify(repairMapper).toRepairResponse(any(AssetTransaction.class));
    }

    @Test
    void createRepair_success_nonSerial_decrementsStock() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(2).build());
        setupMapperToBaseTransaction();
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        DeviceWarehouse dw = new DeviceWarehouse();
        dw.setQuantity(5);
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(10, 2))
                .thenReturn(Optional.of(dw));
        when(repairMapper.toRepairResponse(any(AssetTransaction.class))).thenReturn(new RepairResponse());

        RepairResponse resp = repairService.createRepair(request);

        assertNotNull(resp);
        assertEquals(3, dw.getQuantity());
        verify(deviceWarehouseRepository, atLeastOnce()).save(dw);
    }

    @Test
    void filterRepairs_invalidDateRange_throws() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate from = LocalDate.now();
        LocalDate to = from.minusDays(1);

        CustomException ex = assertThrows(CustomException.class,
                () -> repairService.filterRepairs(null, from, to, pageable));
        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
    }

    @Test
    void filterRepairs_withSearchAndDates_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AssetTransaction> txPage = new PageImpl<>(Collections.singletonList(new AssetTransaction()), pageable, 1);
        when(transactionRepository.findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable))).thenReturn(txPage);
        when(repairMapper.toRepairResponse(any(AssetTransaction.class))).thenReturn(new RepairResponse());

        Page<RepairResponse> page = repairService.filterRepairs("abc", LocalDate.now().minusDays(7), LocalDate.now(), pageable);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable));
    }

    @Test
    void filterRepairs_noFilters_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AssetTransaction> txPage = new PageImpl<>(Collections.singletonList(new AssetTransaction()), pageable, 1);
        when(transactionRepository.findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable))).thenReturn(txPage);
        when(repairMapper.toRepairResponse(any(AssetTransaction.class))).thenReturn(new RepairResponse());

        Page<RepairResponse> page = repairService.filterRepairs(null, null, null, pageable);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
    }
}


