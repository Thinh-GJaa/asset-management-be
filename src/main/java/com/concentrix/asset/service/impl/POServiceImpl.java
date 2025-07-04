package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreatePORequest;
import com.concentrix.asset.dto.request.POItem;
import com.concentrix.asset.dto.response.POResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.POMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.POService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class POServiceImpl implements POService {

    PORepository poRepository;
    POMapper poMapper;
    DeviceRepository deviceRepository;
    PODetailRepository poDetailRepository;
    ModelRepository modelRepository;
    DeviceWarehouseRepository deviceWarehouseRepository;
    UserRepository userRepository;

    @Override
    public POResponse createPO(CreatePORequest createPORequest) {
        log.info("[POServiceImpl] Creating purchase order with request: {}", createPORequest);

        if (poRepository.existsById(createPORequest.getPoId())) {
            throw new CustomException(ErrorCode.PO_ALREADY_EXISTS, createPORequest.getPoId());
        }

        Set<String> serialSet = new HashSet<>();
        for (POItem item : createPORequest.getItems()) {
            String serial = item.getSerialNumber();
            if (serial != null && !serial.isBlank()) {
                if (!serialSet.add(serial)) {
                    throw new CustomException(ErrorCode.DUPLICATE_SERIAL_NUMBER, serial);
                }
            }
        }

        PurchaseOrder purchaseOrder = poMapper.toPurchaseOrder(createPORequest);
        purchaseOrder.setCreatedBy(getCurrentUser());
        purchaseOrder = poRepository.save(purchaseOrder);

        for (POItem item : createPORequest.getItems()) {
            Model model = modelRepository.findById(item.getModelId())
                    .orElseThrow(() -> new CustomException(ErrorCode.MODEL_NOT_FOUND, item.getModelId()));

            if (item.getSerialNumber() != null && !item.getSerialNumber().isBlank()) {
                handleDeviceWithSerial(item, model, purchaseOrder);
            } else {
                handleDeviceWithoutSerial(item, model, purchaseOrder);
            }
        }

        poRepository.save(purchaseOrder);

        return poMapper.toPOResponse(purchaseOrder);
    }

    @Override
    public POResponse getPOById(String poId) {
        return poRepository.findById(poId)
                .map(poMapper::toPOResponse)
                .orElseThrow(() -> new CustomException(ErrorCode.PO_NOT_FOUND, poId));
    }

    @Override
    public Page<POResponse> filterPO(Pageable pageable) {
        return poRepository.findAll(pageable)
                .map(poMapper::toPOResponse);
    }






    private void handleDeviceWithSerial(POItem item, Model model, PurchaseOrder purchaseOrder) {
        if (deviceRepository.findBySerialNumber(item.getSerialNumber()).isPresent()) {
            throw new CustomException(ErrorCode.DEVICE_ALREADY_EXISTS, item.getSerialNumber());
        }
        Device device = Device.builder()
                .serialNumber(item.getSerialNumber())
                .model(model)
                .deviceName(item.getDeviceName())
                .currentWarehouse(purchaseOrder.getWarehouse())
                .currentStatus(com.concentrix.asset.enums.DeviceStatus.AVAILABLE)
                .build();
        device = deviceRepository.save(device);

        createPODetail(purchaseOrder, device, 1);
        createDeviceWarehouse(purchaseOrder.getWarehouse(), device, 1, true);
    }

    private void handleDeviceWithoutSerial(POItem item, Model model, PurchaseOrder purchaseOrder) {
        Device device = Device.builder()
                .model(model)
                .deviceName(item.getDeviceName())
                .currentWarehouse(purchaseOrder.getWarehouse())
                .currentStatus(com.concentrix.asset.enums.DeviceStatus.AVAILABLE)
                .build();
        device = deviceRepository.save(device);

        createPODetail(purchaseOrder, device, item.getQuantity());
        createDeviceWarehouse(purchaseOrder.getWarehouse(), device, item.getQuantity(), false);
    }

    private void createPODetail(PurchaseOrder purchaseOrder, Device device, Integer quantity) {
        PODetail poDetail = PODetail.builder()
                .purchaseOrder(purchaseOrder)
                .device(device)
                .quantity(quantity)
                .build();
        purchaseOrder.getPoDetails().add(poDetail);
    }

    private void createDeviceWarehouse(Warehouse warehouse, Device device, Integer quantity,
            boolean withSerial) {
        if (withSerial) {
            DeviceWarehouse deviceWarehouse = DeviceWarehouse.builder()
                    .warehouse(warehouse)
                    .device(device)
                    .quantity(quantity)
                    .build();
            deviceWarehouseRepository.save(deviceWarehouse);
        } else {
            Integer warehouseId = warehouse.getWarehouseId();
            Integer deviceId = device.getDeviceId();
            DeviceWarehouse deviceWarehouse = deviceWarehouseRepository
                    .findByWarehouse_WarehouseIdAndDevice_DeviceId(warehouseId, deviceId)
                    .orElse(null);
            if (deviceWarehouse != null) {
                deviceWarehouse.setQuantity(deviceWarehouse.getQuantity() + quantity);
                deviceWarehouseRepository.save(deviceWarehouse);
            } else {
                deviceWarehouse = DeviceWarehouse.builder()
                        .warehouse(warehouse)
                        .device(device)
                        .quantity(quantity)
                        .build();
                deviceWarehouseRepository.save(deviceWarehouse);
            }
        }
    }

    private User getCurrentUser() {
        String EID = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));
    }

}