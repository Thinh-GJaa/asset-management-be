package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.response.POItemResponse;
import com.concentrix.asset.dto.response.TransferItemResponse;
import com.concentrix.asset.dto.response.TransferResponse;
import com.concentrix.asset.entity.Floor;
import com.concentrix.asset.entity.TransactionDetail;
import com.concentrix.asset.entity.Warehouse;
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
    public List<TransferItemResponse> mapItems(List<TransactionDetail> details) {
        if (details == null)
            return null;

        return details.stream()
                .map(detail -> TransferItemResponse.builder()
                        .deviceId(detail.getDevice().getDeviceId())
                        .serialNumber(detail.getDevice().getSerialNumber())
                        .deviceName(detail.getDevice().getDeviceName())
                        .quantity(detail.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }
}
