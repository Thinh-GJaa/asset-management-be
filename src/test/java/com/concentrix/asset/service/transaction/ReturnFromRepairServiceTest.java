package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateReturnFromRepairRequest;
import com.concentrix.asset.dto.request.TransactionItem;
import com.concentrix.asset.dto.response.ReturnFromRepairResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.ReturnFromRepairMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.impl.transaction.ReturnFromRepairServiceImpl;
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
class ReturnFromRepairServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private ReturnFromRepairMapper mapper;
    @Mock private UserRepository userRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private DeviceWarehouseRepository deviceWarehouseRepository;
    @Mock private TransactionDetailRepository transactionDetailRepository;

    @InjectMocks private ReturnFromRepairServiceImpl service;

    private CreateReturnFromRepairRequest request;
    private Warehouse toWarehouse;
    private AssetTransaction baseTx;
    private User currentUser;
    private Device serialDevice;
    private Device nonSerialDevice;

    @BeforeEach
    void setup() {
        currentUser = new User();
        currentUser.setEid("E1");

        toWarehouse = new Warehouse();
        toWarehouse.setWarehouseId(10);
        toWarehouse.setWarehouseName("WH-10");

        request = new CreateReturnFromRepairRequest();
        request.setItems(new ArrayList<>());

        baseTx = new AssetTransaction();
        baseTx.setTransactionType(TransactionType.RETURN_FROM_REPAIR);
        baseTx.setToWarehouse(toWarehouse);

        Model model = new Model();
        model.setModelId(99);
        model.setModelName("Model-99");

        serialDevice = new Device();
        serialDevice.setDeviceId(1);
        serialDevice.setSerialNumber("S1");
        serialDevice.setModel(model);

        nonSerialDevice = new Device();
        nonSerialDevice.setDeviceId(2);
        nonSerialDevice.setModel(model);

        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        Mockito.lenient().when(auth.getName()).thenReturn("E1");
        Mockito.lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private void mapRequest() {
        when(mapper.toAssetTransaction(request)).thenReturn(baseTx);
        when(userRepository.findById("E1")).thenReturn(Optional.of(currentUser));
    }

    @Test
    void getById_success() {
        AssetTransaction tx = new AssetTransaction();
        when(transactionRepository.findById(1)).thenReturn(Optional.of(tx));
        when(mapper.toReturnFromRepairResponse(tx)).thenReturn(new ReturnFromRepairResponse());
        ReturnFromRepairResponse resp = service.getReturnFromRepairById(1);
        assertNotNull(resp);
    }

    @Test
    void getById_notFound() {
        when(transactionRepository.findById(404)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.getReturnFromRepairById(404));
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void create_serialNotFound_aggregated() {
        request.getItems().add(TransactionItem.builder().serialNumber("S404").modelId(null).quantity(1).build());
        mapRequest();
        when(deviceRepository.findBySerialNumber("S404")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromRepair(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("S404"));
    }

    @Test
    void create_serialInvalid_lastTxNotRepair() {
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        mapRequest();
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        TransactionDetail last = new TransactionDetail();
        AssetTransaction lastTx = new AssetTransaction();
        lastTx.setTransactionType(TransactionType.RETURN_FROM_FLOOR);
        last.setTransaction(lastTx);
        when(transactionDetailRepository.findFirstByDevice_DeviceIdOrderByTransaction_TransactionIdDesc(1)).thenReturn(last);

        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromRepair(request));
        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("S1"));
    }

    @Test
    void create_modelNotFound_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        mapRequest();
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromRepair(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Model ID: 99"));
    }

    @Test
    void create_success_serial_updatesDevice() {
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(2).build());
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(3).build());
        mapRequest();
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        TransactionDetail last = new TransactionDetail();
        AssetTransaction lastTx = new AssetTransaction();
        lastTx.setTransactionType(TransactionType.REPAIR);
        last.setTransaction(lastTx);
        when(transactionDetailRepository.findFirstByDevice_DeviceIdOrderByTransaction_TransactionIdDesc(1)).thenReturn(last);
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toReturnFromRepairResponse(any(AssetTransaction.class))).thenReturn(new ReturnFromRepairResponse());

        ReturnFromRepairResponse resp = service.createReturnFromRepair(request);
        assertNotNull(resp);
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void create_success_nonSerial_updatesWarehouse_existingAndNew() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(4).build());
        mapRequest();
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        // first call: existing stock
        DeviceWarehouse existing = new DeviceWarehouse(); existing.setQuantity(3);
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(10, 2))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(existing));
        when(mapper.toReturnFromRepairResponse(any(AssetTransaction.class))).thenReturn(new ReturnFromRepairResponse());

        ReturnFromRepairResponse resp = service.createReturnFromRepair(request);
        assertNotNull(resp);
        verify(deviceWarehouseRepository, atLeastOnce()).save(any(DeviceWarehouse.class));
    }

    @Test
    void create_success_nonSerial_createsNewWarehouseRecord() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(2).build());
        mapRequest();
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(10, 2)).thenReturn(Optional.empty());
        when(mapper.toReturnFromRepairResponse(any(AssetTransaction.class))).thenReturn(new ReturnFromRepairResponse());

        ReturnFromRepairResponse resp = service.createReturnFromRepair(request);
        assertNotNull(resp);
        verify(deviceWarehouseRepository, atLeastOnce()).save(any(DeviceWarehouse.class));
    }

    @Test
    void filter_invalidDateRange_throws() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate from = LocalDate.now();
        LocalDate to = from.minusDays(1);
        CustomException ex = assertThrows(CustomException.class,
                () -> service.filterReturnFromRepairs(null, from, to, pageable));
        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
    }

    @Test
    void filter_withFilters_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AssetTransaction> txPage = new PageImpl<>(Collections.singletonList(new AssetTransaction()), pageable, 1);
        when(transactionRepository.findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable))).thenReturn(txPage);
        when(mapper.toReturnFromRepairResponse(any(AssetTransaction.class))).thenReturn(new ReturnFromRepairResponse());

        Page<ReturnFromRepairResponse> page = service.filterReturnFromRepairs("abc", LocalDate.now().minusDays(7), LocalDate.now(), pageable);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable));
    }

    @Test
    void filter_noFilters_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AssetTransaction> txPage = new PageImpl<>(Collections.singletonList(new AssetTransaction()), pageable, 1);
        when(transactionRepository.findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable))).thenReturn(txPage);
        when(mapper.toReturnFromRepairResponse(any(AssetTransaction.class))).thenReturn(new ReturnFromRepairResponse());

        Page<ReturnFromRepairResponse> page = service.filterReturnFromRepairs(null, null, null, pageable);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
    }
}


