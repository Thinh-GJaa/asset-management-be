package com.concentrix.asset.mapper.helper;

import com.concentrix.asset.dto.response.POResponse;
import com.concentrix.asset.dto.response.TransactionItemsResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.repository.FloorRepository;
import com.concentrix.asset.repository.VendorRepository;
import com.concentrix.asset.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class TransactionMapperHelper {

    WarehouseRepository warehouseRepository;
    FloorRepository floorRepository;
    VendorRepository vendorRepository;

    @NonFinal
    @Value("${app.path.upload.handover}")
    String uploadHandoverPath;

    @Named("warehouseIdToWarehouse")
    public Warehouse warehouseIdToWarehouse(Integer warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new CustomException(ErrorCode.WAREHOUSE_NOT_FOUND, warehouseId));
    }

    @Named("floorIdToFloor")
    public Floor floorIdToFloor(Integer floorId) {
        return floorRepository.findById(floorId)
                .orElseThrow(() -> new CustomException(ErrorCode.FLOOR_NOT_FOUND, floorId));
    }

    @Named("mapItems")
    public List<TransactionItemsResponse> mapItems(List<TransactionDetail> details) {
        if (details == null)
            return null;
        return details.stream()
                .map(detail -> TransactionItemsResponse.builder()
                        .deviceId(detail.getDevice().getDeviceId())
                        .serialNumber(detail.getDevice().getSerialNumber())
                        .modelName(detail.getDevice().getModel().getModelName())
                        .deviceName(detail.getDevice().getDeviceName())
                        .quantity(detail.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }

    @Named("mapLocation")
    public String mapLocation(AssetTransaction assetTransaction) {
        if (assetTransaction.getFromWarehouse() != null) {
            return assetTransaction.getFromWarehouse().getSite().getSiteName();
        } else if (assetTransaction.getToWarehouse() != null) {
            return assetTransaction.getToWarehouse().getSite().getSiteName();
        }
        return null;
    }

    @Named("transactionTypeToAssetType")
    public String transactionTypeToAssetType(AssetTransaction assetTransaction) {

        if (assetTransaction.getTransactionType() == TransactionType.ASSIGNMENT) {
            if (assetTransaction.getReturnDate() != null) {
                return "Temporary";
            } else {
                return "Permanent";
            }
        }

        if (assetTransaction.getTransactionType() == TransactionType.RETURN_FROM_USER) {
            return "Return";
        }
        return null;
    }


    @Named("vendorIdToVendor")
    public Vendor vendorIdToVendor(Integer vendorId) {
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> new CustomException(ErrorCode.VENDOR_NOT_FOUND, vendorId));
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

    @Named("mapPOItems")
    public List<TransactionItemsResponse> mapPOItems(List<PODetail> poDetails) {
        if (poDetails == null)
            return null;
        return poDetails.stream()
                .map(detail -> TransactionItemsResponse.builder()
                        .deviceId(detail.getDevice().getDeviceId())
                        .serialNumber(detail.getDevice().getSerialNumber())
                        .modelName(detail.getDevice().getModel().getModelName())
                        .deviceName(detail.getDevice().getDeviceName())
                        .quantity(detail.getQuantity())
                        .build())
                .toList();
    }

    @Named("mapImages")
    public List<String> mapImages(List<TransactionImage> transactionImages) {
        if (transactionImages == null)
            return null;
        return transactionImages.stream()
                .map(TransactionImage::getImageName)
                .toList();

    }

}
