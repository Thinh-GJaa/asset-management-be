package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateAssignmentRequest;
import com.concentrix.asset.dto.response.AssetHandoverResponse;
import com.concentrix.asset.dto.response.AssignmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AssignmentService {

    AssignmentResponse getAssignmentById(Integer assignmentId);

    AssignmentResponse createAssignment(CreateAssignmentRequest request);

    Page<AssignmentResponse> filterAssignments(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    AssetHandoverResponse getAssetHandoverByAssignmentId(Integer assignmentId);
}

