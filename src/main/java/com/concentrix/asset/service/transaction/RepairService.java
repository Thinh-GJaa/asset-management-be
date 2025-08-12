package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateRepairRequest;
import com.concentrix.asset.dto.response.RepairResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface RepairService {

    RepairResponse getRepairById(Integer repairId);

    RepairResponse createRepair(CreateRepairRequest request);

    Page<RepairResponse> filterRepairs(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);

}