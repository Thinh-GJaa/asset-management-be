package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateAssignmentRequest;
import com.concentrix.asset.dto.request.TransactionItem;
import com.concentrix.asset.dto.response.AssetHandoverResponse;
import com.concentrix.asset.dto.response.AssignmentResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.AssignmentMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.UserService;
import com.concentrix.asset.service.impl.transaction.AssignmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssignmentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AssignmentMapper assignmentMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private DeviceWarehouseRepository deviceWarehouseRepository;
    @Mock
    private UserService userService;
    @Mock
    private DeviceUserRepository deviceUserRepository;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    private CreateAssignmentRequest request;
    private User currentUser;
    private User useUser;
    private Warehouse fromWarehouse;
    private Device serialDevice;
    private Device nonSerialDevice;
    private AssetTransaction baseTransaction;

    @BeforeEach
    void init() {
        currentUser = new User();
        currentUser.setEid("E111");
        currentUser.setFullName("Creator");

        useUser = new User();
        useUser.setEid("E123");
        useUser.setFullName("Assignee");

        fromWarehouse = new Warehouse();
        fromWarehouse.setWarehouseId(10);
        fromWarehouse.setWarehouseName("WH-10");

        serialDevice = new Device();
        serialDevice.setDeviceId(1);
        serialDevice.setSerialNumber("S1");
        serialDevice.setStatus(DeviceStatus.IN_STOCK);
        serialDevice.setCurrentWarehouse(fromWarehouse);

        Model model = new Model();
        model.setModelId(99);
        model.setModelName("Model-99");
        nonSerialDevice = new Device();
        nonSerialDevice.setDeviceId(2);
        nonSerialDevice.setModel(model);
        nonSerialDevice.setSerialNumber(null);

        request = new CreateAssignmentRequest();
        request.setFromWarehouseId(10);
        request.setEid("E123");
        request.setReturnDate(null);
        request.setItems(new ArrayList<>());

        baseTransaction = new AssetTransaction();
        baseTransaction.setTransactionId(100);
        baseTransaction.setTransactionType(TransactionType.ASSIGNMENT);
        baseTransaction.setFromWarehouse(fromWarehouse);
        baseTransaction.setUserUse(useUser);
        baseTransaction.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getAssignmentById_success() {
        AssetTransaction tx = new AssetTransaction();
        when(transactionRepository.findById(100)).thenReturn(Optional.of(tx));
        when(assignmentMapper.toAssignmentResponse(tx)).thenReturn(new AssignmentResponse());

        AssignmentResponse response = assignmentService.getAssignmentById(100);

        assertNotNull(response);
        verify(transactionRepository).findById(100);
        verify(assignmentMapper).toAssignmentResponse(tx);
    }

    @Test
    void getAssignmentById_notFound_throws() {
        when(transactionRepository.findById(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> assignmentService.getAssignmentById(999));
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createAssignment_invalidReturnDate_throws() {
        request.setReturnDate(LocalDate.now().minusDays(1));
        when(assignmentMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(userService.getCurrentUser()).thenReturn(currentUser);

        CustomException ex = assertThrows(CustomException.class, () -> assignmentService.createAssignment(request));
        assertEquals(ErrorCode.INVALID_RETURN_DATE, ex.getErrorCode());
    }

    @Test
    void createAssignment_serialNotFound_collectAndThrow() {
        // Item with serial that doesn't exist
        request.getItems().add(TransactionItem.builder().serialNumber("S404").modelId(99).quantity(1).build());
        when(assignmentMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(deviceRepository.findBySerialNumber("S404")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> assignmentService.createAssignment(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("S404"));
    }

    @Test
    void createAssignment_modelIdNotFound_throws() {
        // Item without serial, with modelId
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        when(assignmentMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> assignmentService.createAssignment(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Model ID: 99"));
    }

    @Test
    void createAssignment_missingSerialAndModel_throws() {
        // Build an invalid item (both null)
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(null).quantity(1).build());
        when(assignmentMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(userService.getCurrentUser()).thenReturn(currentUser);

        CustomException ex = assertThrows(CustomException.class, () -> assignmentService.createAssignment(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Either serialNumber or modelId"));
    }

    @Test
    void createAssignment_success_serialDevice_updatesAndReturns() {
        // One serial item OK
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(99).quantity(1).build());
        when(assignmentMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentMapper.toAssignmentResponse(any(AssetTransaction.class))).thenReturn(new AssignmentResponse());

        AssignmentResponse response = assignmentService.createAssignment(request);

        assertNotNull(response);
        verify(deviceRepository).save(any(Device.class));
        verify(transactionRepository).save(any(AssetTransaction.class));
        verify(assignmentMapper).toAssignmentResponse(any(AssetTransaction.class));
    }

    @Test
    void createAssignment_serialDevice_invalidStatus_collectsAndThrowsAfterLoop() {
        // Device not IN_STOCK triggers INVALID_DEVICE_STATUS after processing
        serialDevice.setStatus(DeviceStatus.IN_FLOOR);
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(99).quantity(1).build());
        when(assignmentMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomException ex = assertThrows(CustomException.class, () -> assignmentService.createAssignment(request));
        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("S1"));
        // device save still happens before throw
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void createAssignment_serialDevice_wrongWarehouse_collectsAndThrows() {
        Warehouse other = new Warehouse();
        other.setWarehouseId(20);
        serialDevice.setCurrentWarehouse(other);
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(99).quantity(1).build());
        when(assignmentMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomException ex = assertThrows(CustomException.class, () -> assignmentService.createAssignment(request));
        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("S1"));
    }

    @Test
    void createAssignment_nonSerial_missingStockRecord_throws() {
        // Non-serial path requires DeviceWarehouse stock
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(2).build());
        when(assignmentMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(10, 2))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> assignmentService.createAssignment(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE, ex.getErrorCode());
    }

    @Test
    void createAssignment_nonSerial_insufficientStock_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(5).build());
        DeviceWarehouse dw = new DeviceWarehouse();
        dw.setQuantity(3);
        when(assignmentMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(10, 2))
                .thenReturn(Optional.of(dw));

        CustomException ex = assertThrows(CustomException.class, () -> assignmentService.createAssignment(request));
        assertEquals(ErrorCode.STOCK_OUT, ex.getErrorCode());
    }

    @Test
    void createAssignment_nonSerial_success_updatesStockAndDeviceUser() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(2).build());
        DeviceWarehouse dw = new DeviceWarehouse();
        dw.setQuantity(5);
        when(assignmentMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(10, 2))
                .thenReturn(Optional.of(dw));
        when(deviceUserRepository.findByDevice_DeviceIdAndUser_Eid(2, "E123"))
                .thenReturn(Optional.empty());
        when(assignmentMapper.toAssignmentResponse(any(AssetTransaction.class)))
                .thenReturn(new AssignmentResponse());

        AssignmentResponse response = assignmentService.createAssignment(request);

        assertNotNull(response);
        assertEquals(3, dw.getQuantity()); // decremented in memory
        verify(deviceWarehouseRepository).save(dw);
        verify(deviceUserRepository).save(any(DeviceUser.class));
    }

    @Test
    void filterAssignments_withSearchAndDates_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        AssetTransaction tx = new AssetTransaction();
        tx.setTransactionType(TransactionType.ASSIGNMENT);
        Page<AssetTransaction> page = new PageImpl<>(Collections.singletonList(tx), pageable, 1);
        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(assignmentMapper.toAssignmentResponse(tx)).thenReturn(new AssignmentResponse());

        Page<AssignmentResponse> result = assignmentService.filterAssignments("abc", LocalDate.now().minusDays(7), LocalDate.now(), pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void filterAssignments_noFilters_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        AssetTransaction tx = new AssetTransaction();
        tx.setTransactionType(TransactionType.ASSIGNMENT);
        Page<AssetTransaction> page = new PageImpl<>(Collections.singletonList(tx), pageable, 1);
        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(assignmentMapper.toAssignmentResponse(tx)).thenReturn(new AssignmentResponse());

        Page<AssignmentResponse> result = assignmentService.filterAssignments(null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAssetHandoverByAssignmentId_success_whenTypeAssignment() {
        AssetTransaction tx = new AssetTransaction();
        tx.setTransactionType(TransactionType.ASSIGNMENT);
        when(transactionRepository.findById(100)).thenReturn(Optional.of(tx));
        when(assignmentMapper.toAssetHandoverResponse(tx)).thenReturn(new AssetHandoverResponse());

        AssetHandoverResponse response = assignmentService.getAssetHandoverByAssignmentId(100);

        assertNotNull(response);
        verify(assignmentMapper).toAssetHandoverResponse(tx);
    }

    @Test
    void getAssetHandoverByAssignmentId_notFound_throws() {
        when(transactionRepository.findById(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> assignmentService.getAssetHandoverByAssignmentId(999));
        assertEquals(ErrorCode.ASSIGNMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getAssetHandoverByAssignmentId_wrongType_throws() {
        AssetTransaction tx = new AssetTransaction();
        tx.setTransactionType(TransactionType.REPAIR);
        when(transactionRepository.findById(100)).thenReturn(Optional.of(tx));

        CustomException ex = assertThrows(CustomException.class,
                () -> assignmentService.getAssetHandoverByAssignmentId(100));
        assertEquals(ErrorCode.ASSIGNMENT_NOT_FOUND, ex.getErrorCode());
    }
}
