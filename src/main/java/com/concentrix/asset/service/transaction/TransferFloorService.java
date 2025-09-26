package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateTransferFloorRequest;
import com.concentrix.asset.dto.response.TransferFloorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface TransferFloorService {
    TransferFloorResponse getTransferFloorById(Integer transferFloorId);

    TransferFloorResponse createTransferFloor(CreateTransferFloorRequest request);

    Page<TransferFloorResponse> filterTransferFloors(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);
}