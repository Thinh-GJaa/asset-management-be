package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateDisposalRequest;
import com.concentrix.asset.dto.request.TransactionItem;
import com.concentrix.asset.dto.response.DisposalResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.DisposalMapper;
import com.concentrix.asset.repository.DeviceRepository;
import com.concentrix.asset.repository.DeviceWarehouseRepository;
import com.concentrix.asset.repository.TransactionRepository;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.service.impl.transaction.DisposalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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
public class DisposalServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private DisposalMapper disposalMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private DeviceWarehouseRepository deviceWarehouseRepository;

    @InjectMocks
    private DisposalServiceImpl disposalService;

    private CreateDisposalRequest request;
    private Warehouse fromWarehouse;
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

        request = new CreateDisposalRequest();
        request.setFromWarehouseId(10);
        request.setItems(new ArrayList<>());

        baseTransaction = new AssetTransaction();
        baseTransaction.setTransactionType(TransactionType.DISPOSAL);
        baseTransaction.setFromWarehouse(fromWarehouse);

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
    }

    private void setupSecurityContextMock(MockedStatic<SecurityContextHolder> mockedStatic) {
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        
        when(auth.getName()).thenReturn("E111");
        when(securityContext.getAuthentication()).thenReturn(auth);
        mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
    }

    @Test
    void getDisposalById_success() {
        AssetTransaction tx = new AssetTransaction();
        when(transactionRepository.findById(1)).thenReturn(Optional.of(tx));
        when(disposalMapper.toDisposalResponse(tx)).thenReturn(new DisposalResponse());

        DisposalResponse resp = disposalService.getDisposalById(1);
        assertNotNull(resp);
        verify(transactionRepository).findById(1);
        verify(disposalMapper).toDisposalResponse(tx);
    }

    @Test
    void getDisposalById_notFound_throws() {
        when(transactionRepository.findById(999)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> disposalService.getDisposalById(999));
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createDisposal_duplicateSerial_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        when(disposalMapper.toAssetTransaction(request)).thenReturn(baseTransaction);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            when(userRepository.findById("E111")).thenReturn(Optional.of(currentUser));

            CustomException ex = assertThrows(CustomException.class, () -> disposalService.createDisposal(request));
            assertEquals(ErrorCode.DUPLICATE_SERIAL_NUMBER, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("S1"));
        }
    }

    @Test
    void createDisposal_duplicateModel_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        when(disposalMapper.toAssetTransaction(request)).thenReturn(baseTransaction);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            when(userRepository.findById("E111")).thenReturn(Optional.of(currentUser));

            CustomException ex = assertThrows(CustomException.class, () -> disposalService.createDisposal(request));
            assertEquals(ErrorCode.DUPLICATE_SERIAL_NUMBER, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Model ID: 99"));
        }
    }

    @Test
    void createDisposal_serialNotFound_collectsAndThrows() {
        request.getItems().add(TransactionItem.builder().serialNumber("S404").modelId(null).quantity(1).build());
        when(disposalMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(deviceRepository.findBySerialNumber("S404")).thenReturn(Optional.empty());

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            when(userRepository.findById("E111")).thenReturn(Optional.of(currentUser));

            CustomException ex = assertThrows(CustomException.class, () -> disposalService.createDisposal(request));
            assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("S404"));
        }
    }

    @Test
    void createDisposal_modelNotFound_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        when(disposalMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.empty());

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            when(userRepository.findById("E111")).thenReturn(Optional.of(currentUser));

            CustomException ex = assertThrows(CustomException.class, () -> disposalService.createDisposal(request));
            assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Model ID: 99"));
        }
    }

    @Test
    void createDisposal_missingSerialAndModel_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(null).quantity(1).build());
        when(disposalMapper.toAssetTransaction(request)).thenReturn(baseTransaction);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            when(userRepository.findById("E111")).thenReturn(Optional.of(currentUser));

            CustomException ex = assertThrows(CustomException.class, () -> disposalService.createDisposal(request));
            assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Either serialNumber or modelId"));
        }
    }

    @Test
    void createDisposal_serialInvalidStatus_collectsAndThrows() {
        serialDevice.setStatus(DeviceStatus.IN_FLOOR);
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        when(disposalMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            when(userRepository.findById("E111")).thenReturn(Optional.of(currentUser));

            CustomException ex = assertThrows(CustomException.class, () -> disposalService.createDisposal(request));
            assertEquals(ErrorCode.INVALID_DEVICE_STATUS, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("S1"));
        }
    }

    @Test
    void createDisposal_serialWrongWarehouse_collectsAndThrows() {
        Warehouse other = new Warehouse();
        other.setWarehouseId(20);
        serialDevice.setCurrentWarehouse(other);
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        when(disposalMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            when(userRepository.findById("E111")).thenReturn(Optional.of(currentUser));

            CustomException ex = assertThrows(CustomException.class, () -> disposalService.createDisposal(request));
            assertEquals(ErrorCode.INVALID_DEVICE_STATUS, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("S1"));
        }
    }

    @Test
    void createDisposal_nonSerial_missingStock_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(2).build());
        when(disposalMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(10, 2))
                .thenReturn(Optional.empty());

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            when(userRepository.findById("E111")).thenReturn(Optional.of(currentUser));

            CustomException ex = assertThrows(CustomException.class, () -> disposalService.createDisposal(request));
            assertEquals(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE, ex.getErrorCode());
        }
    }

    @Test
    void createDisposal_nonSerial_insufficientStock_throws() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(5).build());
        DeviceWarehouse dw = new DeviceWarehouse();
        dw.setQuantity(3);
        when(disposalMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(10, 2))
                .thenReturn(Optional.of(dw));

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            when(userRepository.findById("E111")).thenReturn(Optional.of(currentUser));

            CustomException ex = assertThrows(CustomException.class, () -> disposalService.createDisposal(request));
            assertEquals(ErrorCode.STOCK_OUT, ex.getErrorCode());
        }
    }

    @Test
    void createDisposal_success_serial_updatesDeviceAndSaves() {
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        when(disposalMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(disposalMapper.toDisposalResponse(any(AssetTransaction.class))).thenReturn(new DisposalResponse());

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            when(userRepository.findById("E111")).thenReturn(Optional.of(currentUser));

            // updateStockForDisposal will set status to DISPOSED and save device
            DisposalResponse resp = disposalService.createDisposal(request);

            assertNotNull(resp);
            verify(transactionRepository).save(any(AssetTransaction.class));
            verify(deviceRepository).save(any(Device.class));
            verify(disposalMapper).toDisposalResponse(any(AssetTransaction.class));
        }
    }

    @Test
    void createDisposal_success_nonSerial_decrementsStock() {
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(2).build());
        DeviceWarehouse dw = new DeviceWarehouse();
        dw.setQuantity(5);
        when(disposalMapper.toAssetTransaction(request)).thenReturn(baseTransaction);
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.of(nonSerialDevice));
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        // Validate path check
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(10, 2))
                .thenReturn(Optional.of(dw));
        // Called again in updateStockForDisposal
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(10, 2))
                .thenReturn(Optional.of(dw));
        when(disposalMapper.toDisposalResponse(any(AssetTransaction.class))).thenReturn(new DisposalResponse());

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            when(userRepository.findById("E111")).thenReturn(Optional.of(currentUser));

            DisposalResponse resp = disposalService.createDisposal(request);

            assertNotNull(resp);
            assertEquals(3, dw.getQuantity());
            verify(deviceWarehouseRepository, atLeastOnce()).save(dw);
        }
    }

    @Test
    void filterDisposals_invalidDateRange_throws() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate from = LocalDate.now();
        LocalDate to = from.minusDays(1);

        CustomException ex = assertThrows(CustomException.class,
                () -> disposalService.filterDisposals(null, from, to, pageable));
        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
    }

    @Test
    void filterDisposals_withSearchAndDates_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AssetTransaction> txPage = new PageImpl<>(Collections.singletonList(new AssetTransaction()), pageable, 1);
        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(txPage);
        when(disposalMapper.toDisposalResponse(any(AssetTransaction.class))).thenReturn(new DisposalResponse());

        Page<DisposalResponse> page = disposalService.filterDisposals("abc", LocalDate.now().minusDays(7), LocalDate.now(), pageable);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void filterDisposals_noFilters_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AssetTransaction> txPage = new PageImpl<>(Collections.singletonList(new AssetTransaction()), pageable, 1);
        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(txPage);
        when(disposalMapper.toDisposalResponse(any(AssetTransaction.class))).thenReturn(new DisposalResponse());

        Page<DisposalResponse> page = disposalService.filterDisposals(null, null, null, pageable);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
    }
}
