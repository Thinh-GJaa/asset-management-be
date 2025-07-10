package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreatePORequest;
import com.concentrix.asset.dto.response.POItemResponse;
import com.concentrix.asset.dto.response.POResponse;
import com.concentrix.asset.entity.PurchaseOrder;
import com.concentrix.asset.entity.Vendor;
import com.concentrix.asset.entity.Warehouse;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.repository.VendorRepository;
import com.concentrix.asset.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import com.concentrix.asset.entity.PODetail;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.dto.request.POItem;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class POMapperHelper {

    VendorRepository vendorRepository;
    WarehouseRepository warehouseRepository;

    @Named("vendorIdToVendor")
    public Vendor vendorIdToVendor(Integer vendorId) {
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> new CustomException(ErrorCode.VENDOR_NOT_FOUND, vendorId));
    }

    @Named("warehouseIdToWarehouse")
    public Warehouse warehouseIdToWarehouse(Integer warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new CustomException(ErrorCode.WAREHOUSE_NOT_FOUND, warehouseId));
    }

    @Named("mapVendor")
    public POResponse.VendorResponse mapVendor(Vendor vendor) {
        if (vendor == null)
            return null;
        return POResponse.VendorResponse.builder()
                .vendorId(vendor.getVendorId())
                .vendorName(vendor.getVendorName())
                .build();
    }

    @Named("mapWarehouse")
    public POResponse.WarehouseResponse mapWarehouse(Warehouse warehouse) {
        if (warehouse == null)
            return null;
        return POResponse.WarehouseResponse.builder()
                .warehouseId(warehouse.getWarehouseId())
                .warehouseName(warehouse.getWarehouseName())
                .build();
    }

    @Named("mapUser")
    public POResponse.UserResponse mapUser(User user) {
        if (user == null)
            return null;
        return POResponse.UserResponse.builder()
                .eid(user.getEid())
                .fullName(user.getFullName())
                .build();
    }

    @Named("mapPOItems")
    public List<POItemResponse> mapPOItems(List<PODetail> poDetails) {
        if (poDetails == null)
            return null;
        return poDetails.stream()
                .map(detail -> {
                    String deviceName;
                    if (detail.getDevice().getSerialNumber() != null
                            && !detail.getDevice().getSerialNumber().isEmpty()) {
                        // Có serial: sử dụng tên thiết bị cụ thể
                        deviceName = detail.getDevice().getDeviceName();
                    } else {
                        // Không có serial: sử dụng tên model
                        deviceName = detail.getDevice().getModel().getModelName();
                    }

                    return POItemResponse.builder()
                            .deviceId(detail.getDevice().getDeviceId())
                            .serialNumber(detail.getDevice().getSerialNumber())
                            .deviceName(deviceName)
                            .quantity(detail.getQuantity())
                            .build();
                })
                .collect(Collectors.toList());
    }
}