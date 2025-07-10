package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateTransferFloorRequest;
import com.concentrix.asset.dto.response.TransferFloorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransferFloorService {
    TransferFloorResponse getTransferFloorById(Integer transferFloorId);

    TransferFloorResponse createTransferFloor(CreateTransferFloorRequest request);

    Page<TransferFloorResponse> filterTransferFloors(Pageable pageable);
}