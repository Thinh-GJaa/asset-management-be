package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateEWasteRequest;
import com.concentrix.asset.dto.request.TransactionItem;
import com.concentrix.asset.dto.response.EWasteResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.EWasteMapper;
import com.concentrix.asset.repository.DeviceRepository;
import com.concentrix.asset.repository.DeviceWarehouseRepository;
import com.concentrix.asset.repository.TransactionRepository;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.service.impl.transaction.EWasteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EWasteServiceImpl Tests")
class EWasteServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private EWasteMapper ewasteMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceWarehouseRepository deviceWarehouseRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private EWasteServiceImpl ewasteService;

    private User testUser;
    private Warehouse testWarehouse;
    private Model testModel;
    private Device testDevice;
    private AssetTransaction testTransaction;
    private DeviceWarehouse testDeviceWarehouse;
    private CreateEWasteRequest testRequest;
    private TransactionItem testItem;

    @BeforeEach
    void setUp() {
        // Setup test data
        testUser = User.builder()
                .eid("test.user")
                .fullName("Test User")
                .build();

        testWarehouse = Warehouse.builder()
                .warehouseId(1)
                .warehouseName("Test Warehouse")
                .build();

        testModel = Model.builder()
                .modelId(1)
                .modelName("Test Model")
                .type(DeviceType.LAPTOP)
                .build();

        testDevice = Device.builder()
                .deviceId(1)
                .serialNumber("SN123456")
                .deviceName("Test Device")
                .status(DeviceStatus.IN_STOCK)
                .model(testModel)
                .currentWarehouse(testWarehouse)
                .build();

        testDeviceWarehouse = DeviceWarehouse.builder()
                .device(testDevice)
                .warehouse(testWarehouse)
                .quantity(10)
                .build();

        testTransaction = AssetTransaction.builder()
                .transactionId(1)
                .transactionType(TransactionType.E_WASTE)
                .fromWarehouse(testWarehouse)
                .createdBy(testUser)
                .createdAt(LocalDateTime.now())
                .note("Test note")
                .build();

        testItem = TransactionItem.builder()
                .serialNumber("SN123456")
                .modelId(1)
                .quantity(1)
                .build();

        testRequest = new CreateEWasteRequest();
        testRequest.setFromWarehouseId(1);
        testRequest.setNote("Test note");
        testRequest.setItems(Collections.singletonList(testItem));

        // SecurityContext is configured per test via mocked static SecurityContextHolder
    }

    private void setupSecurityContextMock(MockedStatic<SecurityContextHolder> mockedStatic) {
        when(authentication.getName()).thenReturn("test.user");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
    }

    @Test
    @DisplayName("getEWasteById - Should return EWasteResponse when transaction exists")
    void getEWasteById_WhenTransactionExists_ShouldReturnResponse() {
        // Given
        Integer transactionId = 1;
        EWasteResponse expectedResponse = EWasteResponse.builder()
                .transactionId(transactionId)
                .note("Test note")
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(testTransaction));
        when(ewasteMapper.toEWasteResponse(testTransaction)).thenReturn(expectedResponse);

        // When
        EWasteResponse result = ewasteService.getEWasteById(transactionId);

        // Then
        assertNotNull(result);
        assertEquals(transactionId, result.getTransactionId());
        assertEquals("Test note", result.getNote());
        verify(transactionRepository).findById(transactionId);
        verify(ewasteMapper).toEWasteResponse(testTransaction);
    }

    @Test
    @DisplayName("getEWasteById - Should throw CustomException when transaction not found")
    void getEWasteById_WhenTransactionNotFound_ShouldThrowException() {
        // Given
        Integer transactionId = 999;
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(CustomException.class,
            () -> ewasteService.getEWasteById(transactionId));

        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
        assertTrue(exception.getMessage().contains(transactionId.toString()));
        verify(transactionRepository).findById(transactionId);
    }

    @Test
    @DisplayName("createEWaste - Should create E-Waste successfully with serial device")
    void createEWaste_WithSerialDevice_ShouldCreateSuccessfully() {
        // Given
        when(userRepository.findById("test.user")).thenReturn(Optional.of(testUser));
        when(deviceRepository.findBySerialNumber("SN123456")).thenReturn(Optional.of(testDevice));
        when(ewasteMapper.toAssetTransaction(testRequest)).thenReturn(testTransaction);
        when(transactionRepository.save(any(AssetTransaction.class))).thenReturn(testTransaction);
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);
        EWasteResponse mappedResponse = EWasteResponse.builder()
                .transactionId(1)
                .note("Test note")
                .build();
        when(ewasteMapper.toEWasteResponse(any(AssetTransaction.class))).thenReturn(mappedResponse);
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            // When
            EWasteResponse result = ewasteService.createEWaste(testRequest);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTransactionId());
            assertEquals("Test note", result.getNote());
            verify(transactionRepository).save(any(AssetTransaction.class));
            verify(deviceRepository).save(any(Device.class));
            verify(deviceRepository).findBySerialNumber("SN123456");
            verify(ewasteMapper).toEWasteResponse(any(AssetTransaction.class));
        }
    }

    @Test
    @DisplayName("createEWaste - Should create E-Waste successfully with non-serial device")
    void createEWaste_WithNonSerialDevice_ShouldCreateSuccessfully() {
        // Given
        Device nonSerialDevice = Device.builder()
                .deviceId(2)
                .serialNumber(null)
                .deviceName("Non-serial Device")
                .status(DeviceStatus.IN_STOCK)
                .model(testModel)
                .currentWarehouse(testWarehouse)
                .build();

        TransactionItem nonSerialItem = TransactionItem.builder()
                .serialNumber(null)
                .modelId(1)
                .quantity(2)
                .build();

        CreateEWasteRequest nonSerialRequest = new CreateEWasteRequest();
        nonSerialRequest.setFromWarehouseId(1);
        nonSerialRequest.setNote("Test note");
        nonSerialRequest.setItems(Collections.singletonList(nonSerialItem));

        when(userRepository.findById("test.user")).thenReturn(Optional.of(testUser));
        when(deviceRepository.findFirstByModel_ModelId(1)).thenReturn(Optional.of(nonSerialDevice));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(1, 2))
                .thenReturn(Optional.of(testDeviceWarehouse));
        when(ewasteMapper.toAssetTransaction(nonSerialRequest)).thenReturn(testTransaction);
        when(transactionRepository.save(any(AssetTransaction.class))).thenReturn(testTransaction);
        when(deviceWarehouseRepository.save(any(DeviceWarehouse.class))).thenReturn(testDeviceWarehouse);

        EWasteResponse mappedResponse = EWasteResponse.builder()
                .transactionId(1)
                .note("Test note")
                .build();
        when(ewasteMapper.toEWasteResponse(any(AssetTransaction.class))).thenReturn(mappedResponse);
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            // When
            EWasteResponse result = ewasteService.createEWaste(nonSerialRequest);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTransactionId());
            verify(transactionRepository).save(any(AssetTransaction.class));
            verify(deviceWarehouseRepository).save(any(DeviceWarehouse.class));
            verify(ewasteMapper).toEWasteResponse(any(AssetTransaction.class));
        }
    }

    @Test
    @DisplayName("createEWaste - Should throw exception when duplicate serial number")
    void createEWaste_WithDuplicateSerialNumber_ShouldThrowException() {
        // Given
        TransactionItem duplicateItem = TransactionItem.builder()
                .serialNumber("SN123456")
                .modelId(1)
                .quantity(1)
                .build();

        testRequest.setItems(Arrays.asList(testItem, duplicateItem));

        when(userRepository.findById("test.user")).thenReturn(Optional.of(testUser));
        when(ewasteMapper.toAssetTransaction(testRequest)).thenReturn(testTransaction);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                () -> ewasteService.createEWaste(testRequest));

            assertEquals(ErrorCode.DUPLICATE_SERIAL_NUMBER, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("SN123456"));
        }
    }

    @Test
    @DisplayName("createEWaste - Should throw exception when duplicate model ID")
    void createEWaste_WithDuplicateModelId_ShouldThrowException() {
        // Given
        TransactionItem item1 = TransactionItem.builder()
                .serialNumber(null)
                .modelId(1)
                .quantity(1)
                .build();

        TransactionItem item2 = TransactionItem.builder()
                .serialNumber(null)
                .modelId(1)
                .quantity(2)
                .build();

        testRequest.setItems(Arrays.asList(item1, item2));

        when(userRepository.findById("test.user")).thenReturn(Optional.of(testUser));
        when(ewasteMapper.toAssetTransaction(testRequest)).thenReturn(testTransaction);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                () -> ewasteService.createEWaste(testRequest));

            assertEquals(ErrorCode.DUPLICATE_SERIAL_NUMBER, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Model ID: 1"));
        }
    }

    @Test
    @DisplayName("createEWaste - Should throw exception when device not found by serial")
    void createEWaste_WhenDeviceNotFoundBySerial_ShouldThrowException() {
        // Given
        when(userRepository.findById("test.user")).thenReturn(Optional.of(testUser));
        when(deviceRepository.findBySerialNumber("SN123456")).thenReturn(Optional.empty());
        when(ewasteMapper.toAssetTransaction(testRequest)).thenReturn(testTransaction);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                () -> ewasteService.createEWaste(testRequest));

            assertEquals(ErrorCode.DEVICE_NOT_FOUND, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("SN123456"));
        }
    }

    @Test
    @DisplayName("createEWaste - Should throw exception when device not found by model")
    void createEWaste_WhenDeviceNotFoundByModel_ShouldThrowException() {
        // Given
        TransactionItem modelItem = TransactionItem.builder()
                .serialNumber(null)
                .modelId(999)
                .quantity(1)
                .build();

        testRequest.setItems(Collections.singletonList(modelItem));

        when(userRepository.findById("test.user")).thenReturn(Optional.of(testUser));
        when(deviceRepository.findFirstByModel_ModelId(999)).thenReturn(Optional.empty());
        when(ewasteMapper.toAssetTransaction(testRequest)).thenReturn(testTransaction);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                () -> ewasteService.createEWaste(testRequest));

            assertEquals(ErrorCode.DEVICE_NOT_FOUND, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Model ID: 999"));
        }
    }

    @Test
    @DisplayName("createEWaste - Should throw exception when neither serial nor model provided")
    void createEWaste_WhenNeitherSerialNorModelProvided_ShouldThrowException() {
        // Given
        TransactionItem invalidItem = TransactionItem.builder()
                .serialNumber(null)
                .modelId(null)
                .quantity(1)
                .build();

        testRequest.setItems(Collections.singletonList(invalidItem));

        when(userRepository.findById("test.user")).thenReturn(Optional.of(testUser));
        when(ewasteMapper.toAssetTransaction(testRequest)).thenReturn(testTransaction);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                () -> ewasteService.createEWaste(testRequest));

            assertEquals(ErrorCode.DEVICE_NOT_FOUND, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Either serialNumber or modelId must be provided"));
        }
    }

    @Test
    @DisplayName("createEWaste - Should throw exception when device status is not IN_STOCK")
    void createEWaste_WhenDeviceStatusNotInStock_ShouldThrowException() {
        // Given
        Device assignedDevice = Device.builder()
                .deviceId(1)
                .serialNumber("SN123456")
                .deviceName("Test Device")
                .status(DeviceStatus.ASSIGNED)
                .model(testModel)
                .currentWarehouse(testWarehouse)
                .build();

        when(userRepository.findById("test.user")).thenReturn(Optional.of(testUser));
        when(deviceRepository.findBySerialNumber("SN123456")).thenReturn(Optional.of(assignedDevice));
        when(ewasteMapper.toAssetTransaction(testRequest)).thenReturn(testTransaction);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                () -> ewasteService.createEWaste(testRequest));

            assertEquals(ErrorCode.INVALID_DEVICE_STATUS, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("SN123456"));
        }
    }

    @Test
    @DisplayName("createEWaste - Should throw exception when device not in correct warehouse")
    void createEWaste_WhenDeviceNotInCorrectWarehouse_ShouldThrowException() {
        // Given
        Warehouse differentWarehouse = Warehouse.builder()
                .warehouseId(2)
                .warehouseName("Different Warehouse")
                .build();

        Device deviceInDifferentWarehouse = Device.builder()
                .deviceId(1)
                .serialNumber("SN123456")
                .deviceName("Test Device")
                .status(DeviceStatus.IN_STOCK)
                .model(testModel)
                .currentWarehouse(differentWarehouse)
                .build();

        when(userRepository.findById("test.user")).thenReturn(Optional.of(testUser));
        when(deviceRepository.findBySerialNumber("SN123456")).thenReturn(Optional.of(deviceInDifferentWarehouse));
        when(ewasteMapper.toAssetTransaction(testRequest)).thenReturn(testTransaction);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                () -> ewasteService.createEWaste(testRequest));

            assertEquals(ErrorCode.INVALID_DEVICE_STATUS, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("SN123456"));
        }
    }

    @Test
    @DisplayName("createEWaste - Should throw exception when device not found in warehouse for non-serial")
    void createEWaste_WhenDeviceNotFoundInWarehouse_ShouldThrowException() {
        // Given
        Device nonSerialDevice = Device.builder()
                .deviceId(2)
                .serialNumber(null)
                .deviceName("Non-serial Device")
                .status(DeviceStatus.IN_STOCK)
                .model(testModel)
                .currentWarehouse(testWarehouse)
                .build();

        TransactionItem nonSerialItem = TransactionItem.builder()
                .serialNumber(null)
                .modelId(1)
                .quantity(2)
                .build();

        testRequest.setItems(Collections.singletonList(nonSerialItem));

        when(userRepository.findById("test.user")).thenReturn(Optional.of(testUser));
        when(deviceRepository.findFirstByModel_ModelId(1)).thenReturn(Optional.of(nonSerialDevice));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(1, 2))
                .thenReturn(Optional.empty());
        when(ewasteMapper.toAssetTransaction(testRequest)).thenReturn(testTransaction);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                () -> ewasteService.createEWaste(testRequest));

            assertEquals(ErrorCode.DEVICE_NOT_FOUND_IN_WAREHOUSE, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Test Model"));
            assertTrue(exception.getMessage().contains("Test Warehouse"));
        }
    }

    @Test
    @DisplayName("createEWaste - Should throw exception when insufficient stock for non-serial device")
    void createEWaste_WhenInsufficientStock_ShouldThrowException() {
        // Given
        Device nonSerialDevice = Device.builder()
                .deviceId(2)
                .serialNumber(null)
                .deviceName("Non-serial Device")
                .status(DeviceStatus.IN_STOCK)
                .model(testModel)
                .currentWarehouse(testWarehouse)
                .build();

        DeviceWarehouse lowStock = DeviceWarehouse.builder()
                .device(nonSerialDevice)
                .warehouse(testWarehouse)
                .quantity(1) // Less than requested quantity
                .build();

        TransactionItem nonSerialItem = TransactionItem.builder()
                .serialNumber(null)
                .modelId(1)
                .quantity(2)
                .build();

        testRequest.setItems(Collections.singletonList(nonSerialItem));

        when(userRepository.findById("test.user")).thenReturn(Optional.of(testUser));
        when(deviceRepository.findFirstByModel_ModelId(1)).thenReturn(Optional.of(nonSerialDevice));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(1, 2))
                .thenReturn(Optional.of(lowStock));
        when(ewasteMapper.toAssetTransaction(testRequest)).thenReturn(testTransaction);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                () -> ewasteService.createEWaste(testRequest));

            assertEquals(ErrorCode.STOCK_OUT, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Test Model"));
        }
    }

    @Test
    @DisplayName("filterEWastes - Should return filtered results successfully")
    @SuppressWarnings("unchecked")
    void filterEWastes_WithValidFilters_ShouldReturnResults() {
        // Given
        String search = "test";
        LocalDate fromDate = LocalDate.now().minusDays(7);
        LocalDate toDate = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 10);

        List<AssetTransaction> transactions = Collections.singletonList(testTransaction);
        Page<AssetTransaction> transactionPage = new PageImpl<>(transactions, pageable, 1);

        EWasteResponse response = EWasteResponse.builder()
                .transactionId(1)
                .note("Test note")
                .build();

        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(transactionPage);
        when(ewasteMapper.toEWasteResponse(testTransaction)).thenReturn(response);

        Page<EWasteResponse> result = ewasteService.filterEWastes(search, fromDate, toDate, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("filterEWastes - Should throw exception when fromDate is after toDate")
    void filterEWastes_WhenFromDateAfterToDate_ShouldThrowException() {
        // Given
        String search = "test";
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = LocalDate.now().minusDays(7);
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        CustomException exception = assertThrows(CustomException.class,
            () -> ewasteService.filterEWastes(search, fromDate, toDate, pageable));

        assertEquals(ErrorCode.INVALID_DATE_RANGE, exception.getErrorCode());
    }

    @Test
    @DisplayName("filterEWastes - Should handle null search parameter")
    @SuppressWarnings("unchecked")
    void filterEWastes_WithNullSearch_ShouldReturnResults() {
        // Given
        String search = null;
        LocalDate fromDate = null;
        LocalDate toDate = null;
        Pageable pageable = PageRequest.of(0, 10);

        List<AssetTransaction> transactions = Collections.singletonList(testTransaction);
        Page<AssetTransaction> transactionPage = new PageImpl<>(transactions, pageable, 1);

        EWasteResponse response = EWasteResponse.builder()
                .transactionId(1)
                .note("Test note")
                .build();

        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(transactionPage);
        when(ewasteMapper.toEWasteResponse(testTransaction)).thenReturn(response);

        // When
        Page<EWasteResponse> result = ewasteService.filterEWastes(search, fromDate, toDate, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("filterEWastes - Should handle empty search string")
    @SuppressWarnings("unchecked")
    void filterEWastes_WithEmptySearch_ShouldReturnResults() {
        // Given
        String search = "   ";
        LocalDate fromDate = null;
        LocalDate toDate = null;
        Pageable pageable = PageRequest.of(0, 10);

        List<AssetTransaction> transactions = Collections.singletonList(testTransaction);
        Page<AssetTransaction> transactionPage = new PageImpl<>(transactions, pageable, 1);

        EWasteResponse response = EWasteResponse.builder()
                .transactionId(1)
                .note("Test note")
                .build();

        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(transactionPage);
        when(ewasteMapper.toEWasteResponse(testTransaction)).thenReturn(response);

        // When
        Page<EWasteResponse> result = ewasteService.filterEWastes(search, fromDate, toDate, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }



    @Test
    @DisplayName("createEWaste - Should handle mixed serial and non-serial devices")
    void createEWaste_WithMixedDeviceTypes_ShouldHandleBoth() {
        // Given
        Device nonSerialDevice = Device.builder()
                .deviceId(2)
                .serialNumber(null)
                .deviceName("Non-serial Device")
                .status(DeviceStatus.IN_STOCK)
                .model(testModel)
                .currentWarehouse(testWarehouse)
                .build();

        TransactionItem serialItem = TransactionItem.builder()
                .serialNumber("SN123456")
                .modelId(1)
                .quantity(1)
                .build();

        TransactionItem nonSerialItem = TransactionItem.builder()
                .serialNumber(null)
                .modelId(1)
                .quantity(1)
                .build();

        CreateEWasteRequest mixedRequest = new CreateEWasteRequest();
        mixedRequest.setFromWarehouseId(1);
        mixedRequest.setNote("Mixed devices");
        mixedRequest.setItems(Arrays.asList(serialItem, nonSerialItem));

        when(userRepository.findById("test.user")).thenReturn(Optional.of(testUser));
        when(deviceRepository.findBySerialNumber("SN123456")).thenReturn(Optional.of(testDevice));
        when(deviceRepository.findFirstByModel_ModelId(1)).thenReturn(Optional.of(nonSerialDevice));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(1, 2))
                .thenReturn(Optional.of(testDeviceWarehouse));
        when(ewasteMapper.toAssetTransaction(mixedRequest)).thenReturn(testTransaction);
        when(transactionRepository.save(any(AssetTransaction.class))).thenReturn(testTransaction);
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);
        when(deviceWarehouseRepository.save(any(DeviceWarehouse.class))).thenReturn(testDeviceWarehouse);

        EWasteResponse mappedResponse = EWasteResponse.builder()
                .transactionId(1)
                .note("Mixed devices")
                .build();
        when(ewasteMapper.toEWasteResponse(any(AssetTransaction.class))).thenReturn(mappedResponse);
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            // When
            EWasteResponse result = ewasteService.createEWaste(mixedRequest);

            // Then
            assertNotNull(result);
            verify(transactionRepository).save(any(AssetTransaction.class));
            verify(deviceRepository).save(any(Device.class));
            verify(deviceWarehouseRepository).save(any(DeviceWarehouse.class));
            verify(ewasteMapper).toEWasteResponse(any(AssetTransaction.class));
        }
    }

    @Test
    @DisplayName("createEWaste - Should handle empty serial number string")
    void createEWaste_WithEmptySerialNumber_ShouldTreatAsNonSerial() {
        // Given
        Device nonSerialDevice = Device.builder()
                .deviceId(2)
                .serialNumber("")
                .deviceName("Empty Serial Device")
                .status(DeviceStatus.IN_STOCK)
                .model(testModel)
                .currentWarehouse(testWarehouse)
                .build();

        TransactionItem emptySerialItem = TransactionItem.builder()
                .serialNumber("")
                .modelId(1)
                .quantity(1)
                .build();

        testRequest.setItems(Collections.singletonList(emptySerialItem));

        when(userRepository.findById("test.user")).thenReturn(Optional.of(testUser));
        when(deviceRepository.findFirstByModel_ModelId(1)).thenReturn(Optional.of(nonSerialDevice));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(1, 2))
                .thenReturn(Optional.of(testDeviceWarehouse));
        when(ewasteMapper.toAssetTransaction(testRequest)).thenReturn(testTransaction);
        when(transactionRepository.save(any(AssetTransaction.class))).thenReturn(testTransaction);
        when(deviceWarehouseRepository.save(any(DeviceWarehouse.class))).thenReturn(testDeviceWarehouse);

        EWasteResponse mappedResponse = EWasteResponse.builder()
                .transactionId(1)
                .note("Test note")
                .build();
        when(ewasteMapper.toEWasteResponse(any(AssetTransaction.class))).thenReturn(mappedResponse);
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            // When
            EWasteResponse result = ewasteService.createEWaste(testRequest);

            // Then
            assertNotNull(result);
            verify(transactionRepository).save(any(AssetTransaction.class));
            verify(deviceWarehouseRepository).save(any(DeviceWarehouse.class));
            verify(ewasteMapper).toEWasteResponse(any(AssetTransaction.class));
        }
    }

    @Test
    @DisplayName("createEWaste - Should handle device with null current warehouse")
    void createEWaste_WhenDeviceHasNullCurrentWarehouse_ShouldThrowException() {
        // Given
        Device deviceWithNullWarehouse = Device.builder()
                .deviceId(1)
                .serialNumber("SN123456")
                .deviceName("Test Device")
                .status(DeviceStatus.IN_STOCK)
                .model(testModel)
                .currentWarehouse(null)
                .build();

        when(userRepository.findById("test.user")).thenReturn(Optional.of(testUser));
        when(deviceRepository.findBySerialNumber("SN123456")).thenReturn(Optional.of(deviceWithNullWarehouse));
        when(ewasteMapper.toAssetTransaction(testRequest)).thenReturn(testTransaction);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                () -> ewasteService.createEWaste(testRequest));

            assertEquals(ErrorCode.INVALID_DEVICE_STATUS, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("SN123456"));
        }
    }

    @Test
    @DisplayName("createEWaste - Should handle device with null serial number")
    void createEWaste_WhenDeviceHasNullSerialNumber_ShouldTreatAsNonSerial() {
        // Given
        Device deviceWithNullSerial = Device.builder()
                .deviceId(2)
                .serialNumber(null)
                .deviceName("Null Serial Device")
                .status(DeviceStatus.IN_STOCK)
                .model(testModel)
                .currentWarehouse(testWarehouse)
                .build();

        TransactionItem nullSerialItem = TransactionItem.builder()
                .serialNumber(null)
                .modelId(1)
                .quantity(1)
                .build();

        testRequest.setItems(Collections.singletonList(nullSerialItem));

        when(userRepository.findById("test.user")).thenReturn(Optional.of(testUser));
        when(deviceRepository.findFirstByModel_ModelId(1)).thenReturn(Optional.of(deviceWithNullSerial));
        when(deviceWarehouseRepository.findByWarehouse_WarehouseIdAndDevice_DeviceId(1, 2))
                .thenReturn(Optional.of(testDeviceWarehouse));
        when(ewasteMapper.toAssetTransaction(testRequest)).thenReturn(testTransaction);
        when(transactionRepository.save(any(AssetTransaction.class))).thenReturn(testTransaction);
        when(deviceWarehouseRepository.save(any(DeviceWarehouse.class))).thenReturn(testDeviceWarehouse);

        EWasteResponse mappedResponse = EWasteResponse.builder()
                .transactionId(1)
                .note("Test note")
                .build();
        when(ewasteMapper.toEWasteResponse(any(AssetTransaction.class))).thenReturn(mappedResponse);
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            // When
            EWasteResponse result = ewasteService.createEWaste(testRequest);

            // Then
            assertNotNull(result);
            verify(transactionRepository).save(any(AssetTransaction.class));
            verify(deviceWarehouseRepository).save(any(DeviceWarehouse.class));
            verify(ewasteMapper).toEWasteResponse(any(AssetTransaction.class));
        }
    }

    @Test
    @DisplayName("createEWaste - Should handle multiple devices with different statuses")
    void createEWaste_WithMultipleDevicesDifferentStatuses_ShouldHandleCorrectly() {
        // Given
        Device validDevice = Device.builder()
                .deviceId(1)
                .serialNumber("SN123456")
                .deviceName("Valid Device")
                .status(DeviceStatus.IN_STOCK)
                .model(testModel)
                .currentWarehouse(testWarehouse)
                .build();

        Device invalidDevice = Device.builder()
                .deviceId(2)
                .serialNumber("SN789012")
                .deviceName("Invalid Device")
                .status(DeviceStatus.ASSIGNED)
                .model(testModel)
                .currentWarehouse(testWarehouse)
                .build();

        TransactionItem validItem = TransactionItem.builder()
                .serialNumber("SN123456")
                .modelId(1)
                .quantity(1)
                .build();

        TransactionItem invalidItem = TransactionItem.builder()
                .serialNumber("SN789012")
                .modelId(1)
                .quantity(1)
                .build();

        testRequest.setItems(Arrays.asList(validItem, invalidItem));

        when(userRepository.findById("test.user")).thenReturn(Optional.of(testUser));
        when(deviceRepository.findBySerialNumber("SN123456")).thenReturn(Optional.of(validDevice));
        when(deviceRepository.findBySerialNumber("SN789012")).thenReturn(Optional.of(invalidDevice));
        when(ewasteMapper.toAssetTransaction(testRequest)).thenReturn(testTransaction);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            setupSecurityContextMock(mockedStatic);
            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                () -> ewasteService.createEWaste(testRequest));

            assertEquals(ErrorCode.INVALID_DEVICE_STATUS, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("SN789012"));
        }
    }

    @Test
    @DisplayName("filterEWastes - Should handle only fromDate filter")
    @SuppressWarnings("unchecked")
    void filterEWastes_WithOnlyFromDate_ShouldReturnResults() {
        // Given
        String search = null;
        LocalDate fromDate = LocalDate.now().minusDays(7);
        LocalDate toDate = null;
        Pageable pageable = PageRequest.of(0, 10);

        List<AssetTransaction> transactions = Collections.singletonList(testTransaction);
        Page<AssetTransaction> transactionPage = new PageImpl<>(transactions, pageable, 1);

        EWasteResponse response = EWasteResponse.builder()
                .transactionId(1)
                .note("Test note")
                .build();

        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(transactionPage);
        when(ewasteMapper.toEWasteResponse(testTransaction)).thenReturn(response);

        // When
        Page<EWasteResponse> result = ewasteService.filterEWastes(search, fromDate, toDate, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("filterEWastes - Should handle only toDate filter")
    @SuppressWarnings("unchecked")
    void filterEWastes_WithOnlyToDate_ShouldReturnResults() {
        // Given
        String search = null;
        LocalDate fromDate = null;
        LocalDate toDate = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 10);

        List<AssetTransaction> transactions = Collections.singletonList(testTransaction);
        Page<AssetTransaction> transactionPage = new PageImpl<>(transactions, pageable, 1);

        EWasteResponse response = EWasteResponse.builder()
                .transactionId(1)
                .note("Test note")
                .build();

        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(transactionPage);
        when(ewasteMapper.toEWasteResponse(testTransaction)).thenReturn(response);

        // When
        Page<EWasteResponse> result = ewasteService.filterEWastes(search, fromDate, toDate, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }
}
