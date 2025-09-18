package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateTransferRequest;
import com.concentrix.asset.dto.request.TransactionItem;
import com.concentrix.asset.dto.response.TransferResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.TransferMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.UserService;
import com.concentrix.asset.service.impl.transaction.TransferServiceImpl;
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

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private TransferMapper transferMapper;
    @Mock private DeviceRepository deviceRepository;
    @Mock private DeviceWarehouseRepository deviceWarehouseRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private UserService userService;

    @InjectMocks private TransferServiceImpl service;

    private CreateTransferRequest request;
    private Warehouse fromWarehouse;
    private Warehouse toWarehouse;
    private AssetTransaction baseTx;
    private User currentUser;
    private Device serialDevice;
    private Device nonSerialDevice;

    @BeforeEach
    void setup() {
        currentUser = new User(); currentUser.setEid("E1");

        Site site1 = new Site(); site1.setSiteId(100);
        Site site2 = new Site(); site2.setSiteId(200);

        fromWarehouse = new Warehouse(); fromWarehouse.setWarehouseId(1); fromWarehouse.setWarehouseName("W1");
        fromWarehouse.setSite(site1);

        toWarehouse = new Warehouse(); toWarehouse.setWarehouseId(2); toWarehouse.setWarehouseName("W2");
        toWarehouse.setSite(site2);

        request = new CreateTransferRequest();
        request.setFromWarehouseId(1);
        request.setToWarehouseId(2);
        request.setItems(new ArrayList<>());

        baseTx = new AssetTransaction();
        baseTx.setTransactionType(TransactionType.TRANSFER_SITE);
        baseTx.setFromWarehouse(fromWarehouse);
        baseTx.setToWarehouse(toWarehouse);

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

//        when(userService.getCurrentUser()).thenReturn(currentUser);
    }

    private void mapBase() {
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(fromWarehouse));
        when(warehouseRepository.findById(2)).thenReturn(Optional.of(toWarehouse));
        when(transferMapper.toAssetTransaction(request)).thenReturn(baseTx);
    }

    @Test
    void getTransferById_success_and_notFound() {
        AssetTransaction tx = new AssetTransaction();
        when(transactionRepository.findById(1)).thenReturn(Optional.of(tx));
        when(transferMapper.toTransferResponse(tx)).thenReturn(new TransferResponse());
        assertNotNull(service.getTransferById(1));
        when(transactionRepository.findById(404)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.getTransferById(404));
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createTransfer_invalidWarehouses_sameSite() {
        // Same site warehouses
        Site sameSite = new Site(); sameSite.setSiteId(100);
        toWarehouse.setSite(sameSite);
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(fromWarehouse));
        when(warehouseRepository.findById(2)).thenReturn(Optional.of(toWarehouse));
        when(transferMapper.toAssetTransaction(request)).thenReturn(baseTx);
        
        CustomException ex = assertThrows(CustomException.class, () -> service.createTransfer(request));
        assertEquals(ErrorCode.INVALID_SITE_TRANSFER, ex.getErrorCode());
    }

    @Test
    void createTransfer_serialNotFound_aggregated() {
        mapBase();
        request.getItems().add(TransactionItem.builder().serialNumber("S404").modelId(null).quantity(1).build());
        request.getItems().add(TransactionItem.builder().serialNumber("S405").modelId(null).quantity(1).build());
        when(deviceRepository.findBySerialNumber("S404")).thenReturn(Optional.empty());
        when(deviceRepository.findBySerialNumber("S405")).thenReturn(Optional.empty());
        
        CustomException ex = assertThrows(CustomException.class, () -> service.createTransfer(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("S404") && ex.getMessage().contains("S405"));
    }

    @Test
    void createTransfer_modelNotFound_and_missingIds() {
        mapBase();
        // Model not found
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.empty());
        CustomException ex1 = assertThrows(CustomException.class, () -> service.createTransfer(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex1.getErrorCode());

        // Missing both serial and model
        request.setItems(new ArrayList<>());
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(null).quantity(1).build());
        CustomException ex2 = assertThrows(CustomException.class, () -> service.createTransfer(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex2.getErrorCode());
    }

    @Test
    void createTransfer_serialInvalidWarehouse() {
        mapBase();
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        // Device not in from warehouse
        Warehouse otherWarehouse = new Warehouse(); otherWarehouse.setWarehouseId(999);
        serialDevice.setCurrentWarehouse(otherWarehouse);
        
        CustomException ex = assertThrows(CustomException.class, () -> service.createTransfer(request));
        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, ex.getErrorCode());
    }

    @Test
    void createTransfer_nonSerial_missingOrInsufficientStock() {
        mapBase();
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(5).build());
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        
        // Missing from warehouse record
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(1, 20)).thenReturn(Optional.empty());
        CustomException ex1 = assertThrows(CustomException.class, () -> service.createTransfer(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE, ex1.getErrorCode());

        // Insufficient stock
        DeviceWarehouse fromStock = new DeviceWarehouse(); fromStock.setQuantity(3);
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(1, 20)).thenReturn(Optional.of(fromStock));
        CustomException ex2 = assertThrows(CustomException.class, () -> service.createTransfer(request));
        assertEquals(ErrorCode.STOCK_OUT, ex2.getErrorCode());
    }

    @Test
    void createTransfer_success_updatesDevicesAndWarehouses() {
        mapBase();
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(2).build());
        
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        
        DeviceWarehouse fromStock = new DeviceWarehouse(); fromStock.setQuantity(5);
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(1, 20)).thenReturn(Optional.of(fromStock));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transferMapper.toTransferResponse(any(AssetTransaction.class))).thenReturn(new TransferResponse());

        TransferResponse resp = service.createTransfer(request);
        assertNotNull(resp);
        verify(deviceRepository).save(serialDevice);
        verify(deviceWarehouseRepository).save(fromStock);
        assertEquals(DeviceStatus.ON_THE_MOVE, serialDevice.getStatus());
        assertEquals(3, fromStock.getQuantity()); // 5 - 2
    }

    @Test
    void approveTransfer_validationAndExpectedError() {
        AssetTransaction tx = new AssetTransaction();
        tx.setTransactionType(TransactionType.TRANSFER_SITE);
        tx.setTransactionStatus(TransactionStatus.PENDING);
        
        when(transactionRepository.findById(1)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        
        service.approveTransfer(1);
        verify(transactionRepository).save(tx);
        assertEquals(TransactionStatus.APPROVED, tx.getTransactionStatus());
        assertTrue(tx.getConfirmedBy() == null || tx.getConfirmedBy().equals(currentUser));

        // Wrong transaction type
        tx.setTransactionType(TransactionType.REPAIR);
        CustomException ex1 = assertThrows(CustomException.class, () -> service.approveTransfer(1));
        assertEquals(ErrorCode.TRANSACTION_TYPE_INVALID, ex1.getErrorCode());

        // Wrong status
        tx.setTransactionType(TransactionType.TRANSFER_SITE);
        tx.setTransactionStatus(TransactionStatus.APPROVED);
        CustomException ex2 = assertThrows(CustomException.class, () -> service.approveTransfer(1));
        assertEquals(ErrorCode.TRANSACTION_STATUS_INVALID, ex2.getErrorCode());

        // Not found
        when(transactionRepository.findById(404)).thenReturn(Optional.empty());
        CustomException ex3 = assertThrows(CustomException.class, () -> service.approveTransfer(404));
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, ex3.getErrorCode());
    }

    @Test
    void approveTransferByToken_delegatesToApproveTransfer() {
        AssetTransaction tx = new AssetTransaction();
        tx.setTransactionType(TransactionType.TRANSFER_SITE);
        tx.setTransactionStatus(TransactionStatus.PENDING);
        
        when(transactionRepository.findById(123)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        
        service.approveTransferByToken("123");
        verify(transactionRepository).save(tx);
        assertEquals(TransactionStatus.APPROVED, tx.getTransactionStatus());
    }

    @Test
    void confirmTransfer_validationAndDeviceUpdates() {
        AssetTransaction tx = new AssetTransaction();
        tx.setTransactionType(TransactionType.TRANSFER_SITE);
        tx.setTransactionStatus(TransactionStatus.APPROVED);
        tx.setToWarehouse(toWarehouse);
        
        TransactionDetail serialDetail = new TransactionDetail();
        serialDetail.setDevice(serialDevice);
        serialDetail.setQuantity(1);
        
        TransactionDetail nonSerialDetail = new TransactionDetail();
        nonSerialDetail.setDevice(nonSerialDevice);
        nonSerialDetail.setQuantity(2);
        
        tx.setDetails(Arrays.asList(serialDetail, nonSerialDetail));
        
        when(transactionRepository.findById(1)).thenReturn(Optional.of(tx));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(2, 20)).thenReturn(Optional.empty());
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        
        service.confirmTransfer(1);
        
        verify(deviceRepository).save(serialDevice);
        verify(deviceWarehouseRepository).save(any(DeviceWarehouse.class));
        verify(transactionRepository).save(tx);
        assertEquals(TransactionStatus.CONFIRMED, tx.getTransactionStatus());
        assertEquals(DeviceStatus.IN_STOCK, serialDevice.getStatus());
        assertEquals(toWarehouse, serialDevice.getCurrentWarehouse());

        // Wrong transaction type
        tx.setTransactionType(TransactionType.REPAIR);
        CustomException ex1 = assertThrows(CustomException.class, () -> service.confirmTransfer(1));
        assertEquals(ErrorCode.TRANSACTION_TYPE_INVALID, ex1.getErrorCode());

        // Wrong status
        tx.setTransactionType(TransactionType.TRANSFER_SITE);
        tx.setTransactionStatus(TransactionStatus.PENDING);
        CustomException ex2 = assertThrows(CustomException.class, () -> service.confirmTransfer(1));
        assertEquals(ErrorCode.TRANSACTION_STATUS_INVALID, ex2.getErrorCode());

        // Not found
        when(transactionRepository.findById(404)).thenReturn(Optional.empty());
        CustomException ex3 = assertThrows(CustomException.class, () -> service.confirmTransfer(404));
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, ex3.getErrorCode());
    }

    @Test
    void filterTransfers_invalidDateAndValidMapping() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Invalid date range
        LocalDate from = LocalDate.now(); LocalDate to = from.minusDays(1);
        CustomException ex = assertThrows(CustomException.class, () -> service.filterTransfers(null, from, to, pageable));
        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());

        // Valid filter
        Page<AssetTransaction> txPage = new PageImpl<>(Collections.singletonList(new AssetTransaction()), pageable, 1);
        when(transactionRepository.findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable))).thenReturn(txPage);
        when(transferMapper.toTransferResponse(any(AssetTransaction.class))).thenReturn(new TransferResponse());
        
        Page<TransferResponse> page = service.filterTransfers("search", LocalDate.now().minusDays(7), LocalDate.now(), pageable);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable));
    }

    @Test
    void filterTransfersSitePending_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AssetTransaction> txPage = new PageImpl<>(Collections.singletonList(new AssetTransaction()), pageable, 1);
        when(transactionRepository.findPendingOrApprovedTransfers(pageable)).thenReturn(txPage);
        when(transferMapper.toTransferResponse(any(AssetTransaction.class))).thenReturn(new TransferResponse());
        
        Page<TransferResponse> page = service.filterTransfersSitePending(pageable);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findPendingOrApprovedTransfers(pageable);
    }
}
