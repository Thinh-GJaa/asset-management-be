package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateTransferFloorRequest;
import com.concentrix.asset.dto.request.TransactionItem;
import com.concentrix.asset.dto.response.TransferFloorResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.TransferFloorMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.DeviceService;
import com.concentrix.asset.service.impl.transaction.TransferFloorServiceImpl;
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
class TransferFloorServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private TransferFloorMapper mapper;
    @Mock private DeviceRepository deviceRepository;
    @Mock private FloorRepository floorRepository;
    @Mock private UserRepository userRepository;
    @Mock private DeviceService deviceService;
    @Mock private DeviceFloorRepository deviceFloorRepository;

    @InjectMocks private TransferFloorServiceImpl service;

    private CreateTransferFloorRequest request;
    private Floor fromFloor;
    private Floor toFloor;
    private AssetTransaction baseTx;
    private User currentUser;
    private Device serialDevice;
    private Device nonSerialDevice;

    @BeforeEach
    void setup() {
        currentUser = new User(); currentUser.setEid("E1");

        fromFloor = new Floor(); fromFloor.setFloorId(1); fromFloor.setFloorName("F1");
        Site s1 = new Site(); s1.setSiteId(100); fromFloor.setSite(s1);

        toFloor = new Floor(); toFloor.setFloorId(2); toFloor.setFloorName("F2");
        Site s2 = new Site(); s2.setSiteId(100); toFloor.setSite(s2);

        request = new CreateTransferFloorRequest();
        request.setFromFloorId(1);
        request.setToFloorId(2);
        request.setItems(new ArrayList<>());

        baseTx = new AssetTransaction();
        baseTx.setTransactionType(TransactionType.TRANSFER_FLOOR);
        baseTx.setFromFloor(fromFloor);
        baseTx.setToFloor(toFloor);

        Model model = new Model(); model.setModelId(99); model.setModelName("Model-99");

        serialDevice = new Device();
        serialDevice.setDeviceId(10);
        serialDevice.setSerialNumber("S1");
        serialDevice.setStatus(DeviceStatus.IN_FLOOR);
        serialDevice.setCurrentFloor(fromFloor);
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
        when(floorRepository.findById(1)).thenReturn(Optional.of(fromFloor));
        when(floorRepository.findById(2)).thenReturn(Optional.of(toFloor));
        when(mapper.toAssetTransaction(request)).thenReturn(baseTx);
        when(userRepository.findById("E1")).thenReturn(Optional.of(currentUser));
    }

    @Test
    void getById_success_and_notFound() {
        AssetTransaction tx = new AssetTransaction();
        when(transactionRepository.findById(1)).thenReturn(Optional.of(tx));
        when(mapper.toTransferFloorResponse(tx)).thenReturn(new TransferFloorResponse());
        assertNotNull(service.getTransferFloorById(1));
        when(transactionRepository.findById(404)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.getTransferFloorById(404));
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void create_invalid_floors_same_or_diff_site() {
        when(floorRepository.findById(1)).thenReturn(Optional.of(fromFloor));
        when(floorRepository.findById(2)).thenReturn(Optional.of(fromFloor)); // same id
        CustomException ex1 = assertThrows(CustomException.class, () -> service.createTransferFloor(request));
        assertEquals(ErrorCode.INVALID_FLOOR_TRANSFER, ex1.getErrorCode());

        // different sites
        Floor diffSite = new Floor(); diffSite.setFloorId(2); Site other = new Site(); other.setSiteId(200); diffSite.setSite(other);
        when(floorRepository.findById(1)).thenReturn(Optional.of(fromFloor));
        when(floorRepository.findById(2)).thenReturn(Optional.of(diffSite));
        CustomException ex2 = assertThrows(CustomException.class, () -> service.createTransferFloor(request));
        assertEquals(ErrorCode.INVALID_FLOOR_TRANSFER, ex2.getErrorCode());
    }

    @Test
    void create_duplicates_serial_model() {
        mapBase();
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        CustomException ex1 = assertThrows(CustomException.class, () -> service.createTransferFloor(request));
        assertEquals(ErrorCode.DUPLICATE_SERIAL_NUMBER, ex1.getErrorCode());

        request.setItems(new ArrayList<>());
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        CustomException ex2 = assertThrows(CustomException.class, () -> service.createTransferFloor(request));
        assertEquals(ErrorCode.DUPLICATE_SERIAL_NUMBER, ex2.getErrorCode());
    }

    @Test
    void create_serial_notFound_invalidStatus_wrongFloor() {
        mapBase();
        // not found
        request.getItems().add(TransactionItem.builder().serialNumber("S404").modelId(null).quantity(1).build());
        when(deviceRepository.findBySerialNumber("S404")).thenReturn(Optional.empty());
        CustomException exNF = assertThrows(CustomException.class, () -> service.createTransferFloor(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, exNF.getErrorCode());

        // invalid status
        request.setItems(new ArrayList<>());
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        serialDevice.setStatus(DeviceStatus.IN_STOCK);
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        CustomException exIS = assertThrows(CustomException.class, () -> service.createTransferFloor(request));
        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, exIS.getErrorCode());

        // wrong floor
        request.setItems(new ArrayList<>());
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        serialDevice.setStatus(DeviceStatus.IN_FLOOR);
        Floor other = new Floor(); other.setFloorId(999);
        serialDevice.setCurrentFloor(other);
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        CustomException exWF = assertThrows(CustomException.class, () -> service.createTransferFloor(request));
        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, exWF.getErrorCode());
    }

    @Test
    void create_model_notFound_or_missingIds() {
        mapBase();
        // model not found
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(99).quantity(1).build());
        when(deviceRepository.findFirstByModel_ModelId(99)).thenReturn(Optional.empty());
        CustomException exMN = assertThrows(CustomException.class, () -> service.createTransferFloor(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, exMN.getErrorCode());

        // missing ids
        request.setItems(new ArrayList<>());
        request.getItems().add(TransactionItem.builder().serialNumber(null).modelId(null).quantity(1).build());
        CustomException exMI = assertThrows(CustomException.class, () -> service.createTransferFloor(request));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, exMI.getErrorCode());
    }

    @Test
    void create_success_serial_updatesDeviceAndHostname() {
        mapBase();
        request.getItems().add(TransactionItem.builder().serialNumber("S1").modelId(null).quantity(1).build());
        when(deviceRepository.findBySerialNumber("S1")).thenReturn(Optional.of(serialDevice));
        when(deviceService.generateHostNameForDesktop(eq(serialDevice), any(Floor.class))).thenReturn("HOST-F2");
        when(transactionRepository.save(any(AssetTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toTransferFloorResponse(any(AssetTransaction.class))).thenReturn(new TransferFloorResponse());

        TransferFloorResponse resp = service.createTransferFloor(request);
        assertNotNull(resp);
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void filter_cases() {
        Pageable pageable = PageRequest.of(0, 10);
        // invalid date
        LocalDate from = LocalDate.now(); LocalDate to = from.minusDays(1);
        CustomException ex = assertThrows(CustomException.class, () -> service.filterTransferFloors(null, from, to, pageable));
        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());

        // with filters
        Page<AssetTransaction> txPage = new PageImpl<>(Collections.singletonList(new AssetTransaction()), pageable, 1);
        when(transactionRepository.findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable))).thenReturn(txPage);
        when(mapper.toTransferFloorResponse(any(AssetTransaction.class))).thenReturn(new TransferFloorResponse());
        Page<TransferFloorResponse> page = service.filterTransferFloors("abc", LocalDate.now().minusDays(7), LocalDate.now(), pageable);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findAll(Mockito.<Specification<AssetTransaction>>any(), eq(pageable));
    }
}


