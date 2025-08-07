package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.response.TransactionItemsResponse;
import com.concentrix.asset.dto.response.TransferItemResponse;
import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.entity.Floor;
import com.concentrix.asset.entity.TransactionDetail;
import com.concentrix.asset.entity.Warehouse;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class TransactionMapperHelper {

    WarehouseRepository warehouseRepository;
    FloorRepository floorRepository;

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
        if (assetTransaction.getFromWarehouse() != null ) {
            return assetTransaction.getFromWarehouse().getSite().getSiteName();
        } else if (assetTransaction.getToWarehouse() != null ) {
            return assetTransaction.getToWarehouse().getSite().getSiteName();
        }
        return null;
    }

    @Named("transactionTypeToAssetType")
    public String transactionTypeToAssetType(AssetTransaction assetTransaction) {

        if( assetTransaction.getTransactionType() == TransactionType.ASSIGNMENT) {
            if(assetTransaction.getReturnDate() != null) {
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
}
