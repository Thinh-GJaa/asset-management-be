package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateTransferFloorRequest;
import com.concentrix.asset.dto.response.TransferFloorResponse;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransferFloorService {
    TransferFloorResponse getTransferFloorById(Integer transferFloorId);

    TransferFloorResponse createTransferFloor(CreateTransferFloorRequest request);

    Page<TransferFloorResponse> filterTransferFloors(Integer transactionId, java.time.LocalDateTime fromDate, java.time.LocalDateTime toDate, Pageable pageable);
}