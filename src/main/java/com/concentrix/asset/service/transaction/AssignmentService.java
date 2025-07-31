package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateAssignmentRequest;
import com.concentrix.asset.dto.response.AssetHandoverResponse;
import com.concentrix.asset.dto.response.AssignmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssignmentService {

    AssignmentResponse getAssignmentById(Integer assignmentId);

    AssignmentResponse createAssignment(CreateAssignmentRequest request);

    Page<AssignmentResponse> filterAssignments(Pageable pageable);

    AssetHandoverResponse getAssetHandoverByAssignmentId(Integer assignmentId);
}

