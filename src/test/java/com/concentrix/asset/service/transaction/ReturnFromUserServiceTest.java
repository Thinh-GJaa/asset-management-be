package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateReturnFromUserRequest;
import com.concentrix.asset.dto.request.TransactionItem;
import com.concentrix.asset.dto.response.AssetHandoverResponse;
import com.concentrix.asset.dto.response.ReturnFromUserResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.AssignmentMapper;
import com.concentrix.asset.mapper.ReturnFromUserMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.impl.transaction.ReturnFromUserServiceImpl;
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
class ReturnFromUserServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private ReturnFromUserMapper mapper;
    @Mock private UserRepository userRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private DeviceWarehouseRepository deviceWarehouseRepository;
    @Mock private TransactionDetailRepository transactionDetailRepository;
    @Mock private AssignmentMapper assignmentMapper;
    @Mock private DeviceUserRepository deviceUserRepository;

    @InjectMocks private ReturnFromUserServiceImpl service;

    private CreateReturnFromUserRequest request;
    private Warehouse toWarehouse;
    private AssetTransaction baseTx;
    private User currentUser;
    private User borrower;
    private Device serialDevice;
    private Device nonSerialDevice;

    @BeforeEach
    void setup() {
        currentUser = new User(); currentUser.setEid("E1");
        borrower = new User(); borrower.setEid("U100"); borrower.setFullName("Borrower");

        toWarehouse = new Warehouse(); toWarehouse.setWarehouseId(10); toWarehouse.setWarehouseName("WH-10");

        request = new CreateReturnFromUserRequest();
        request.setEid("U100");
        request.setItems(new ArrayList<>());

        baseTx = new AssetTransaction();
        baseTx.setTransactionType(TransactionType.RETURN_FROM_USER);
        baseTx.setToWarehouse(toWarehouse);

        Model model = new Model(); model.setModelId(99); model.setModelName("Model-99");

        serialDevice = new Device();
        serialDevice.setDeviceId(1);
        serialDevice.setSerialNumber("S1");
        serialDevice.setStatus(DeviceStatus.ASSIGNED);
        serialDevice.setCurrentUser(borrower);
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
        when(mapper.toReturnFromUserResponse(tx)).thenReturn(new ReturnFromUserResponse());
        ReturnFromUserResponse resp = service.getReturnFromUserById(1);
        assertNotNull(resp);
    }

    @Test
    void getById_notFound() {
        when(transactionRepository.findById(404)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.getReturnFromUserById(404));
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void create_duplicateSerial_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        mapRequest();
        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromUser(request));
        assertEquals(ErrorCode.DUPLICATE_SERIAL_NUMBER, ex.getErrorCode());
    }

    @Test
    void create_duplicateModel_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        mapRequest();
        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromUser(request));
        assertEquals(ErrorCode.DUPLICATE_SERIAL_NUMBER, ex.getErrorCode());
    }

    @Test
    void create_serialNotFound_aggregated() {
        request.getItems().add(TransactionItem.builder().serialNumber("S404").modelId(null).quantity(1).build());
        mapRequest();
        when(deviceRepository.findBySerialNumber("S404")).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromUser(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("S404"));
    }

    @Test
    void create_modelNotFound_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        mapRequest();
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromUser(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Model ID: 99"));
    }

    @Test
    void create_missingSerialAndModel_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(null).quantity(1).build());
        mapRequest();
        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromUser(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Either serialNumber or modelId"));
    }

    @Test
    void create_serialInvalidStatusOrWrongUser_collectsAndThrows() {
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        mapRequest();
        serialDevice.setStatus(DeviceStatus.IN_STOCK); // invalid status
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromUser(request));
        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, ex.getErrorCode());
    }

    @Test
    void create_serialWrongUser_collectsAndThrows() {
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        mapRequest();
        serialDevice.setStatus(DeviceStatus.ASSIGNED);
        User other = new User(); other.setEid("U200");
        serialDevice.setCurrentUser(other);
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromUser(request));
        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, ex.getErrorCode());
    }

    @Test
    void create_nonSerial_exceedsBorrowed_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(5).build());
        mapRequest();
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        List<TransactionDetail> history = new ArrayList<>();
        AssetTransaction assignTx = new AssetTransaction(); assignTx.setTransactionType(TransactionType.ASSIGNMENT);
        AssetTransaction returnTx = new AssetTransaction(); returnTx.setTransactionType(TransactionType.RETURN_FROM_USER);
        TransactionDetail d1 = new TransactionDetail(); d1.setTransaction(assignTx); d1.setQuantity(3);
        TransactionDetail d2 = new TransactionDetail(); d2.setTransaction(returnTx); d2.setQuantity(1);
        history.add(d1); history.add(d2);
        when(transactionDetailRepository.findAllByDevice_DeviceIdAndTransaction_UserUse_Eid(2, "U100")).thenReturn(history);
        CustomException ex = assertThrows(CustomException.class, () -> service.createReturnFromUser(request));
        assertEquals(ErrorCode.RETURN_QUANTITY_EXCEEDS_BORROWED, ex.getErrorCode());
    }

    @Test
    void create_success_serial_updatesDevice() {
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        mapRequest();
        when(userRepository.findById("U100")).thenReturn(Optional.of(borrower));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        when(mapper.toReturnFromUserResponse(any(AssetTransaction.class))).thenReturn(new ReturnFromUserResponse());

        ReturnFromUserResponse resp = service.createReturnFromUser(request);
        assertNotNull(resp);
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void create_success_nonSerial_updatesWarehouseAndDeviceUser() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(2).build());
        mapRequest();
        when(userRepository.findById("U100")).thenReturn(Optional.of(borrower));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        
        // Mock transaction history: user borrowed 5, returned 1, so currently borrowed = 4
        List<TransactionDetail> history = new ArrayList<>();
        AssetTransaction assignTx = new AssetTransaction(); 
        assignTx.setTransactionType(TransactionType.ASSIGNMENT);
        TransactionDetail assignDetail = new TransactionDetail(); 
        assignDetail.setTransaction(assignTx); 
        assignDetail.setQuantity(5);
        history.add(assignDetail);
        
        AssetTransaction returnTx = new AssetTransaction(); 
        returnTx.setTransactionType(TransactionType.RETURN_FROM_USER);
        TransactionDetail returnDetail = new TransactionDetail(); 
        returnDetail.setTransaction(returnTx); 
        returnDetail.setQuantity(1);
        history.add(returnDetail);
        
        when(transactionDetailRepository.findAllByDevice_DeviceIdAndTransaction_UserUse_Eid(2, "U100")).thenReturn(history);
        
        DeviceWarehouse dw = new DeviceWarehouse(); dw.setQuantity(1);
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(10, 2)).thenReturn(Optional.of(dw));
        DeviceUser deviceUser = new DeviceUser(); deviceUser.setQuantity(4); // Currently borrowed = 4
        when(deviceUserRepository.findByDevice_DeviceIdAndUser_Eid(2, "U100")).thenReturn(Optional.of(deviceUser));
        when(mapper.toReturnFromUserResponse(any(AssetTransaction.class))).thenReturn(new ReturnFromUserResponse());

        ReturnFromUserResponse resp = service.createReturnFromUser(request);
        assertNotNull(resp);
        assertEquals(3, dw.getQuantity()); // 1 + 2 = 3
        assertEquals(2, deviceUser.getQuantity()); // 4 - 2 = 2
        verify(deviceWarehouseRepository).save(dw);
        verify(deviceUserRepository).save(deviceUser);
    }

    @Test
    void create_success_nonSerial_createsWarehouseRecord() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(2).build());
        mapRequest();
        when(userRepository.findById("U100")).thenReturn(Optional.of(borrower));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        
        // Mock transaction history: user borrowed 3, returned 0, so currently borrowed = 3
        List<TransactionDetail> history = new ArrayList<>();
        AssetTransaction assignTx = new AssetTransaction(); 
        assignTx.setTransactionType(TransactionType.ASSIGNMENT);
        TransactionDetail assignDetail = new TransactionDetail(); 
        assignDetail.setTransaction(assignTx); 
        assignDetail.setQuantity(3);
        history.add(assignDetail);
        
        when(transactionDetailRepository.findAllByDevice_DeviceIdAndTransaction_UserUse_Eid(2, "U100")).thenReturn(history);
        
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(10, 2)).thenReturn(Optional.empty());
        DeviceUser deviceUser = new DeviceUser(); deviceUser.setQuantity(3); // Currently borrowed = 3
        when(deviceUserRepository.findByDevice_DeviceIdAndUser_Eid(2, "U100")).thenReturn(Optional.of(deviceUser));
        when(mapper.toReturnFromUserResponse(any(AssetTransaction.class))).thenReturn(new ReturnFromUserResponse());

        ReturnFromUserResponse resp = service.createReturnFromUser(request);
        assertNotNull(resp);
        verify(deviceWarehouseRepository, atLeastOnce()).save(any(DeviceWarehouse.class));
        verify(deviceUserRepository).save(deviceUser);
    }

    @Test
    void filter_invalidDateRange_throws() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate from = LocalDate.now();
        LocalDate to = from.minusDays(1);
        CustomException ex = assertThrows(CustomException.class,
                () -> service.filterReturnFromUsers(null, from, to, pageable));
        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
    }

    @Test
    void filter_withFilters_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AssetTransaction> txPage = new PageImpl<>(Collections.singletonList(new AssetTransaction()), pageable, 1);
        when(transactionRepository.findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable))).thenReturn(txPage);
        when(mapper.toReturnFromUserResponse(any(AssetTransaction.class))).thenReturn(new ReturnFromUserResponse());

        Page<ReturnFromUserResponse> page = service.filterReturnFromUsers("abc", LocalDate.now().minusDays(7), LocalDate.now(), pageable);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable));
    }

    @Test
    void filter_noFilters_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AssetTransaction> txPage = new PageImpl<>(Collections.singletonList(new AssetTransaction()), pageable, 1);
        when(transactionRepository.findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable))).thenReturn(txPage);
        when(mapper.toReturnFromUserResponse(any(AssetTransaction.class))).thenReturn(new ReturnFromUserResponse());

        Page<ReturnFromUserResponse> page = service.filterReturnFromUsers(null, null, null, pageable);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void getAssetHandoverForm_success_and_wrongType() {
        AssetTransaction tx = new AssetTransaction();
        tx.setTransactionType(TransactionType.RETURN_FROM_USER);
        when(transactionRepository.findById(1)).thenReturn(Optional.of(tx));
        when(assignmentMapper.toAssetHandoverResponse(tx)).thenReturn(new AssetHandoverResponse());
        assertNotNull(service.getAssetHandoverForm(1));

        AssetTransaction other = new AssetTransaction();
        other.setTransactionType(TransactionType.REPAIR);
        when(transactionRepository.findById(2)).thenReturn(Optional.of(other));
        CustomException ex = assertThrows(CustomException.class, () -> service.getAssetHandoverForm(2));
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, ex.getErrorCode());
    }
}


