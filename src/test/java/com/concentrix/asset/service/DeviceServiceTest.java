package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.UpdateDeviceRequest;
import com.concentrix.asset.dto.request.UpdateSeatNumberRequest;
import com.concentrix.asset.dto.response.DeviceResponse;
import com.concentrix.asset.dto.response.DeviceMovementHistoryResponse;
import com.concentrix.asset.dto.response.DeviceBorrowingInfoResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.DeviceMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.impl.DeviceServiceImpl;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeviceServiceTest {
	@Mock
	private DeviceRepository deviceRepository;
	@Mock
	private DeviceMapper deviceMapper;
	@Mock
	private ModelRepository modelRepository;
	@Mock
	private TransactionDetailRepository transactionDetailRepository;
	@Mock
	private PODetailRepository poDetailRepository;
	@Mock
	private TransactionRepository transactionRepository;

	@InjectMocks
	private DeviceServiceImpl deviceService;

	private Device device;
	private Model model;
	private User user;
	private Floor floor;
	private Site site;
	private Account account;
	private DeviceResponse deviceResponse;
	private UpdateDeviceRequest updateDeviceRequest;
	private UpdateSeatNumberRequest updateSeatNumberRequest;

	@BeforeEach
	void setUp() {
		// Setup basic entities
		user = new User();
		user.setEid("E123");
		user.setFullName("Test User");

		account = new Account();
		account.setAccountCode("TEST");

		site = new Site();
		site.setSiteId(1);
		site.setSiteName("QTSC1");

		floor = new Floor();
		floor.setFloorId(1);
		floor.setFloorName("Floor 1");
		floor.setSite(site);
		floor.setAccount(account);

		model = new Model();
		model.setModelId(1);
		model.setModelName("Test Model");
		model.setType(DeviceType.LAPTOP);
		model.setManufacturer("Dell");

		device = new Device();
		device.setDeviceId(1);
		device.setDeviceName("Test Device");
		device.setSerialNumber("ABC123XYZ");
		device.setStatus(DeviceStatus.IN_FLOOR);
		device.setModel(model);
		device.setCurrentFloor(floor);
		device.setCurrentUser(user);

		deviceResponse = new DeviceResponse();
		deviceResponse.setDeviceId(1);
		deviceResponse.setDeviceName("Test Device");

		updateDeviceRequest = new UpdateDeviceRequest();
		updateDeviceRequest.setDeviceId(1);
		updateDeviceRequest.setModelId(1);
		updateDeviceRequest.setSerialNumber("SN123");

		updateSeatNumberRequest = new UpdateSeatNumberRequest();
		updateSeatNumberRequest.setSerialNumber("SN123");
		updateSeatNumberRequest.setSeatNumber("SEAT1");
	}

	@Test
	void getDeviceById_existingDevice_returnsDeviceResponse() {
		when(deviceRepository.findById(1)).thenReturn(Optional.of(device));
		when(deviceMapper.toDeviceResponse(device)).thenReturn(deviceResponse);

		DeviceResponse response = deviceService.getDeviceById(1);

		assertNotNull(response);
		verify(deviceRepository).findById(1);
		verify(deviceMapper).toDeviceResponse(device);
	}

	@Test
	void getDeviceById_nonExistingDevice_throwsCustomException() {
		when(deviceRepository.findById(999)).thenReturn(Optional.empty());

		CustomException exception = assertThrows(CustomException.class,
				() -> deviceService.getDeviceById(999));

		assertEquals(ErrorCode.DEVICE_NOT_FOUND, exception.getErrorCode());
		verify(deviceRepository).findById(999);
		verify(deviceMapper, never()).toDeviceResponse(any());
	}

	@Test
	void updateDevice_validRequest_returnsUpdatedDevice() {
		when(deviceRepository.findById(1)).thenReturn(Optional.of(device));
		when(modelRepository.findById(1)).thenReturn(Optional.of(model));
		when(deviceRepository.findBySerialNumber("SN123")).thenReturn(Optional.empty());
		when(deviceRepository.save(device)).thenReturn(device);
		when(deviceMapper.toDeviceResponse(device)).thenReturn(deviceResponse);

		DeviceResponse response = deviceService.updateDevice(updateDeviceRequest);

		assertNotNull(response);
		verify(deviceRepository).findById(1);
		verify(modelRepository).findById(1);
		verify(deviceRepository).findBySerialNumber("SN123");
		verify(deviceMapper).updateDevice(device, updateDeviceRequest);
		verify(deviceRepository).save(device);
	}

	@Test
	void updateDevice_deviceNotFound_throwsCustomException() {
		// Ensure request uses the missing ID
		updateDeviceRequest.setDeviceId(999);
		when(deviceRepository.findById(999)).thenReturn(Optional.empty());

		CustomException exception = assertThrows(CustomException.class,
				() -> deviceService.updateDevice(updateDeviceRequest));

		assertEquals(ErrorCode.DEVICE_NOT_FOUND, exception.getErrorCode());
	}

	@Test
	void updateDevice_modelNotFound_throwsCustomException() {
		when(deviceRepository.findById(1)).thenReturn(Optional.of(device));
		// Ensure request uses the missing model ID
		updateDeviceRequest.setModelId(999);
		when(modelRepository.findById(999)).thenReturn(Optional.empty());

		CustomException exception = assertThrows(CustomException.class,
				() -> deviceService.updateDevice(updateDeviceRequest));

		assertEquals(ErrorCode.MODEL_NOT_FOUND, exception.getErrorCode());
		verify(deviceRepository).findById(1);
		verify(modelRepository).findById(999);
	}

	@Test
	void updateDevice_serialNumberAlreadyExists_throwsCustomException() {
		Device existingDevice = new Device();
		existingDevice.setSerialNumber("SN123");
		existingDevice.setDeviceId(2);

		when(deviceRepository.findById(1)).thenReturn(Optional.of(device));
		when(modelRepository.findById(1)).thenReturn(Optional.of(model));
		when(deviceRepository.findBySerialNumber("SN123")).thenReturn(Optional.of(existingDevice));

		CustomException exception = assertThrows(CustomException.class,
			() -> deviceService.updateDevice(updateDeviceRequest));

		assertEquals(ErrorCode.SERIAL_NUMBER_ALREADY_EXISTS, exception.getErrorCode());
		verify(deviceRepository).findBySerialNumber("SN123");
	}

	@Test
	void updateDevice_sameSerialNumber_success() {
		updateDeviceRequest.setSerialNumber("ABC123XYZ"); // Same as existing
		when(deviceRepository.findById(1)).thenReturn(Optional.of(device));
		when(modelRepository.findById(1)).thenReturn(Optional.of(model));
		when(deviceRepository.findBySerialNumber("ABC123XYZ")).thenReturn(Optional.of(device));
		when(deviceRepository.save(device)).thenReturn(device);
		when(deviceMapper.toDeviceResponse(device)).thenReturn(deviceResponse);

		DeviceResponse response = deviceService.updateDevice(updateDeviceRequest);

		assertNotNull(response);
		verify(deviceRepository).findBySerialNumber("ABC123XYZ");
		verify(deviceMapper).updateDevice(device, updateDeviceRequest);
	}

	@Test
	void updateDevice_nullSerialNumber_success() {
		updateDeviceRequest.setSerialNumber(null);
		when(deviceRepository.findById(1)).thenReturn(Optional.of(device));
		when(modelRepository.findById(1)).thenReturn(Optional.of(model));
		when(deviceRepository.save(device)).thenReturn(device);
		when(deviceMapper.toDeviceResponse(device)).thenReturn(deviceResponse);

		DeviceResponse response = deviceService.updateDevice(updateDeviceRequest);

		assertNotNull(response);
		verify(deviceRepository, never()).findBySerialNumber(any());
		verify(deviceMapper).updateDevice(device, updateDeviceRequest);
	}

	@Test
	void filterDevices_validSearch_returnsFilteredDevices() {
		Page<Device> devicePage = new PageImpl<>(Collections.singletonList(device));
		Pageable pageable = PageRequest.of(0, 10);

		when(deviceRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(devicePage);
		when(deviceMapper.toDeviceResponse(device)).thenReturn(deviceResponse);

		Page<DeviceResponse> result = deviceService.filterDevices("test", DeviceType.LAPTOP, 1, DeviceStatus.IN_FLOOR, pageable);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
	}

	@Test
	void filterDevices_emptySearchAndNullFilters_returnsAllDevices() {
		Page<Device> devicePage = new PageImpl<>(Collections.singletonList(device));
		Pageable pageable = PageRequest.of(0, 10);

		when(deviceRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(devicePage);
		when(deviceMapper.toDeviceResponse(device)).thenReturn(deviceResponse);

		Page<DeviceResponse> result = deviceService.filterDevices(null, null, null, null, pageable);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		verify(deviceRepository).findAll(any(Specification.class), eq(pageable));
	}

	@Test
	void filterDevices_whitespaceSearch_returnsAllDevices() {
		Page<Device> devicePage = new PageImpl<>(Collections.singletonList(device));
		Pageable pageable = PageRequest.of(0, 10);

		when(deviceRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(devicePage);
		when(deviceMapper.toDeviceResponse(device)).thenReturn(deviceResponse);

		Page<DeviceResponse> result = deviceService.filterDevices("   ", null, null, null, pageable);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
	}

	@Test
	void filterDevicesNonSeatNumber_validSearch_returnsFilteredDevices() {
		Page<Device> devicePage = new PageImpl<>(Collections.singletonList(device));
		Pageable pageable = PageRequest.of(0, 10);

		when(deviceRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(devicePage);
		when(deviceMapper.toDeviceResponse(device)).thenReturn(deviceResponse);

		Page<DeviceResponse> result = deviceService.filterDevicesNonSeatNumber("test", 1, 1, pageable);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		verify(deviceRepository).findAll(any(Specification.class), eq(pageable));
	}

	@Test
	void filterDevicesNonSeatNumber_nullParameters_returnsAllDevices() {
		Page<Device> devicePage = new PageImpl<>(Collections.singletonList(device));
		Pageable pageable = PageRequest.of(0, 10);

		when(deviceRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(devicePage);
		when(deviceMapper.toDeviceResponse(device)).thenReturn(deviceResponse);

		Page<DeviceResponse> result = deviceService.filterDevicesNonSeatNumber(null, null, null, pageable);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
	}

	@Test
	void getDeviceMovementHistoryBySerial_deviceNotFound_throwsCustomException() {
		when(deviceRepository.findBySerialNumber("SN999")).thenReturn(Optional.empty());

		CustomException exception = assertThrows(CustomException.class,
			() -> deviceService.getDeviceMovementHistoryBySerial("SN999"));

		assertEquals(ErrorCode.DEVICE_NOT_FOUND, exception.getErrorCode());
		verify(deviceRepository).findBySerialNumber("SN999");
	}

	@Test
	void getDeviceMovementHistoryBySerial_withPODetails_returnsHistory() {
		AssetTransaction transaction = new AssetTransaction();
		transaction.setTransactionId(1);
		transaction.setTransactionType(TransactionType.ASSIGNMENT);
		transaction.setCreatedAt(LocalDateTime.now());
		transaction.setCreatedBy(user);

		TransactionDetail detail = new TransactionDetail();
		detail.setTransaction(transaction);
		detail.setDevice(device);

		PurchaseOrder po = new PurchaseOrder();
		po.setCreatedAt(LocalDate.now());
		po.setCreatedBy(user);
		po.setVendor(new Vendor());
		po.setWarehouse(new Warehouse());

		PODetail poDetail = new PODetail();
		poDetail.setPurchaseOrder(po);
		poDetail.setDevice(device);

		when(deviceRepository.findBySerialNumber("ABC123XYZ")).thenReturn(Optional.of(device));
		when(poDetailRepository.findByDevice_DeviceIdAndDevice_SerialNumberNotNull(1)).thenReturn(poDetail);
		when(transactionDetailRepository.findAllByDevice_DeviceIdOrderByTransaction_TransactionIdAsc(1))
			.thenReturn(Collections.singletonList(detail));

		List<DeviceMovementHistoryResponse> result = deviceService.getDeviceMovementHistoryBySerial("ABC123XYZ");

		assertNotNull(result);
		assertFalse(result.isEmpty());
		verify(deviceRepository).findBySerialNumber("ABC123XYZ");
		verify(poDetailRepository).findByDevice_DeviceIdAndDevice_SerialNumberNotNull(1);
	}

	@Test
	void getDeviceMovementHistoryBySerial_withoutPODetails_returnsHistory() {
		AssetTransaction transaction = new AssetTransaction();
		transaction.setTransactionId(1);
		transaction.setTransactionType(TransactionType.ASSIGNMENT);
		transaction.setCreatedAt(LocalDateTime.now());
		transaction.setCreatedBy(user);

		TransactionDetail detail = new TransactionDetail();
		detail.setTransaction(transaction);
		detail.setDevice(device);

		when(deviceRepository.findBySerialNumber("ABC123XYZ")).thenReturn(Optional.of(device));
		when(poDetailRepository.findByDevice_DeviceIdAndDevice_SerialNumberNotNull(1)).thenReturn(null);
		when(transactionDetailRepository.findAllByDevice_DeviceIdOrderByTransaction_TransactionIdAsc(1))
			.thenReturn(Collections.singletonList(detail));

		List<DeviceMovementHistoryResponse> result = deviceService.getDeviceMovementHistoryBySerial("ABC123XYZ");

		assertNotNull(result);
		assertFalse(result.isEmpty());
		verify(deviceRepository).findBySerialNumber("ABC123XYZ");
	}

	@Test
	void getAllUserBorrowingDevices_returnsBorrowingInfo() {
		List<Device> allDevices = Collections.singletonList(device);
		AssetTransaction transaction = new AssetTransaction();
		transaction.setTransactionId(1);
		transaction.setTransactionType(TransactionType.ASSIGNMENT);
		transaction.setUserUse(user);
		transaction.setCreatedAt(LocalDateTime.now());

		TransactionDetail detail = new TransactionDetail();
		detail.setTransaction(transaction);
		detail.setDevice(device);

		when(deviceRepository.findAll()).thenReturn(allDevices);
		when(transactionDetailRepository.findAllByDevice_DeviceIdOrderByTransaction_TransactionIdAsc(1))
			.thenReturn(Collections.singletonList(detail));

		List<DeviceBorrowingInfoResponse> result = deviceService.getAllUserBorrowingDevices();

		assertNotNull(result);
		verify(deviceRepository).findAll();
		verify(transactionDetailRepository).findAllByDevice_DeviceIdOrderByTransaction_TransactionIdAsc(1);
	}

	@Test
	void getAllUserBorrowingDevices_withReturnTransaction_returnsEmpty() {
		List<Device> allDevices = Collections.singletonList(device);
		AssetTransaction assignTx = new AssetTransaction();
		assignTx.setTransactionId(1);
		assignTx.setTransactionType(TransactionType.ASSIGNMENT);
		assignTx.setUserUse(user);

		AssetTransaction returnTx = new AssetTransaction();
		returnTx.setTransactionId(2);
		returnTx.setTransactionType(TransactionType.RETURN_FROM_USER);
		returnTx.setUserUse(user);

		TransactionDetail assignDetail = new TransactionDetail();
		assignDetail.setTransaction(assignTx);
		assignDetail.setDevice(device);

		TransactionDetail returnDetail = new TransactionDetail();
		returnDetail.setTransaction(returnTx);
		returnDetail.setDevice(device);

		when(deviceRepository.findAll()).thenReturn(allDevices);
		when(transactionDetailRepository.findAllByDevice_DeviceIdOrderByTransaction_TransactionIdAsc(1))
			.thenReturn(Arrays.asList(assignDetail, returnDetail));

		List<DeviceBorrowingInfoResponse> result = deviceService.getAllUserBorrowingDevices();

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void getBorrowingDevicesByUser_returnsDeviceInfo() {
		List<Device> devicesByEid = Collections.singletonList(device);
		Object[] deviceData = {device, 2L};

		when(deviceRepository.findAllByCurrentUser_Eid("E123")).thenReturn(devicesByEid);
		when(transactionDetailRepository.getDeviceAndQuantityByEid("E123"))
			.thenReturn(Collections.singletonList(deviceData));

		List<DeviceBorrowingInfoResponse.DeviceInfo> result = deviceService.getBorrowingDevicesByUser("E123");

		assertNotNull(result);
		assertFalse(result.isEmpty());
		verify(deviceRepository).findAllByCurrentUser_Eid("E123");
		verify(transactionDetailRepository).getDeviceAndQuantityByEid("E123");
	}

	@Test
	void getBorrowingDevice_returnsPagedBorrowingInfo() {
		List<User> users = Collections.singletonList(user);
		Pageable pageable = PageRequest.of(0, 10);

		when(transactionRepository.findDistinctEidFromTransactions()).thenReturn(users);
		when(deviceRepository.findAllByCurrentUser_Eid("E123")).thenReturn(Collections.emptyList());
		when(transactionDetailRepository.getDeviceAndQuantityByEid("E123")).thenReturn(Collections.emptyList());

		Page<DeviceBorrowingInfoResponse> result = deviceService.getBorrowingDevice(pageable);

		assertNotNull(result);
		verify(transactionRepository).findDistinctEidFromTransactions();
	}

	@Test
	void getAllDeviceTypes_returnsAllTypes() {
		List<String> result = deviceService.getAllDeviceTypes();

		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertTrue(result.contains("LAPTOP"));
		assertTrue(result.contains("DESKTOP"));
	}

	@Test
	void getDeviceStatuses_returnsAllStatuses() {
		List<String> result = deviceService.getDeviceStatuses();

		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertTrue(result.contains("IN_STOCK"));
		assertTrue(result.contains("IN_FLOOR"));
	}

	@Test
	void generateHostNameForLaptop_validDevice_returnsHostName() {
		String hostName = deviceService.generateHostNameForLaptop(device);

		assertEquals("VNHCM-LAPABC123", hostName);
	}

	@Test
	void generateHostNameForLaptop_nonLaptopDevice_returnsNull() {
		model.setType(DeviceType.DESKTOP);
		device.setModel(model);

		String hostName = deviceService.generateHostNameForLaptop(device);

		assertNull(hostName);
	}

	@Test
	void generateHostNameForLaptop_nullSerialNumber_throwsNullPointerException() {
		device.setSerialNumber(null);

		assertThrows(NullPointerException.class, () -> deviceService.generateHostNameForLaptop(device));
	}

	@Test
	void generateHostNameForLaptop_nonDellManufacturer_returnsCorrectHostName() {
		model.setManufacturer("HP");
		device.setSerialNumber("ABC123XYZ");

		String hostName = deviceService.generateHostNameForLaptop(device);

		assertEquals("VNHCM-LAP123XYZ", hostName);
	}

	@Test
	void generateHostNameForDesktop_validDevice_returnsHostName() {
		model.setType(DeviceType.DESKTOP);
		device.setModel(model);

		String hostName = deviceService.generateHostNameForDesktop(device, floor);

		assertEquals("VNQUA-TESTABC123", hostName);
	}

	@Test
	void generateHostNameForDesktop_nonDesktopDevice_returnsNull() {
		model.setType(DeviceType.LAPTOP);
		device.setModel(model);

		String hostName = deviceService.generateHostNameForDesktop(device, floor);

		assertNull(hostName);
	}

	@Test
	void generateHostNameForDesktop_graAccount_returnsCorrectHostName() {
		model.setType(DeviceType.DESKTOP);
		account.setAccountCode("GRA");
		floor.setAccount(account);
		device.setModel(model);

		String hostName = deviceService.generateHostNameForDesktop(device, floor);

		assertEquals("ITVNCNXABC123-D", hostName);
	}

	@Test
	void generateHostNameForDesktop_differentSites_returnsCorrectHostName() {
		model.setType(DeviceType.DESKTOP);
		site.setSiteName("ONEHUB");
		floor.setSite(site);
		device.setModel(model);

		String hostName = deviceService.generateHostNameForDesktop(device, floor);

		assertEquals("VNONE-TESTABC123", hostName);
	}

	@Test
	void updateSeatNumber_validRequests_updatesDevices() {
		when(deviceRepository.findBySerialNumber("SN123")).thenReturn(Optional.of(device));
		when(deviceRepository.findBySeatNumber("SEAT1")).thenReturn(Optional.empty());

		assertDoesNotThrow(() -> deviceService.updateSeatNumber(Collections.singletonList(updateSeatNumberRequest)));
		verify(deviceRepository).saveAll(anyList());
	}

	@Test
	void updateSeatNumber_invalidDeviceStatus_throwsCustomException() {
		device.setStatus(DeviceStatus.IN_STOCK);
		when(deviceRepository.findBySerialNumber("SN123")).thenReturn(Optional.of(device));

		CustomException exception = assertThrows(CustomException.class,
			() -> deviceService.updateSeatNumber(Collections.singletonList(updateSeatNumberRequest)));

		assertEquals(ErrorCode.INVALID_DEVICE_STATUS, exception.getErrorCode());
		verify(deviceRepository, never()).saveAll(anyList());
	}

	@Test
	void updateSeatNumber_deviceNotFound_throwsCustomException() {
		when(deviceRepository.findBySerialNumber("SN123")).thenReturn(Optional.empty());

		CustomException exception = assertThrows(CustomException.class,
			() -> deviceService.updateSeatNumber(Collections.singletonList(updateSeatNumberRequest)));

		assertEquals(ErrorCode.DEVICE_NOT_FOUND, exception.getErrorCode());
		verify(deviceRepository, never()).saveAll(anyList());
	}

	@Test
	void updateSeatNumber_seatNumberAlreadyExists_throwsCustomException() {
		Device existingDevice = new Device();
		existingDevice.setSerialNumber("SN456");
		existingDevice.setSeatNumber("SEAT1");

		when(deviceRepository.findBySerialNumber("SN123")).thenReturn(Optional.of(device));
		when(deviceRepository.findBySeatNumber("SEAT1")).thenReturn(Optional.of(existingDevice));

		CustomException exception = assertThrows(CustomException.class,
			() -> deviceService.updateSeatNumber(Collections.singletonList(updateSeatNumberRequest)));

		assertEquals(ErrorCode.SEAT_NUMBER_ALREADY_EXISTS, exception.getErrorCode());
		verify(deviceRepository, never()).saveAll(anyList());
	}

	@Test
	void updateSeatNumber_multipleDevices_partialSuccess() {
		UpdateSeatNumberRequest request1 = new UpdateSeatNumberRequest();
		request1.setSerialNumber("SN123");
		request1.setSeatNumber("SEAT1");

		UpdateSeatNumberRequest request2 = new UpdateSeatNumberRequest();
		request2.setSerialNumber("SN456");
		request2.setSeatNumber("SEAT2");

		Device device2 = new Device();
		device2.setSerialNumber("SN456");
		device2.setStatus(DeviceStatus.IN_FLOOR);

		when(deviceRepository.findBySerialNumber("SN123")).thenReturn(Optional.of(device));
		when(deviceRepository.findBySerialNumber("SN456")).thenReturn(Optional.of(device2));
		when(deviceRepository.findBySeatNumber("SEAT1")).thenReturn(Optional.empty());
		when(deviceRepository.findBySeatNumber("SEAT2")).thenReturn(Optional.empty());

		assertDoesNotThrow(() -> deviceService.updateSeatNumber(Arrays.asList(request1, request2)));
		verify(deviceRepository).saveAll(anyList());
	}

	@Test
	void updateSeatNumber_multipleDevices_withErrors_throwsException() {
		UpdateSeatNumberRequest request1 = new UpdateSeatNumberRequest();
		request1.setSerialNumber("SN123");
		request1.setSeatNumber("SEAT1");

		UpdateSeatNumberRequest request2 = new UpdateSeatNumberRequest();
		request2.setSerialNumber("SN999");
		request2.setSeatNumber("SEAT2");

		when(deviceRepository.findBySerialNumber("SN123")).thenReturn(Optional.of(device));
		when(deviceRepository.findBySerialNumber("SN999")).thenReturn(Optional.empty());
		when(deviceRepository.findBySeatNumber("SEAT1")).thenReturn(Optional.empty());

		CustomException exception = assertThrows(CustomException.class,
			() -> deviceService.updateSeatNumber(Arrays.asList(request1, request2)));

		assertEquals(ErrorCode.DEVICE_NOT_FOUND, exception.getErrorCode());
		assertTrue(exception.getMessage().contains("SN999"));
		verify(deviceRepository, never()).saveAll(anyList());
	}
}
