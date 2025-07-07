package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateAssignmentRequest;
import com.concentrix.asset.dto.response.AssignmentResponse;
import com.concentrix.asset.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/transaction/assignment")
public class AssignmentController {

    AssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request){
        ApiResponse<AssignmentResponse> apiResponse = ApiResponse.<AssignmentResponse>builder()
                .message("Transaction assignment created successful")
                .data(assignmentService.createAssignment(request))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getById(
            @PathVariable Integer id){
        ApiResponse<AssignmentResponse> apiResponse = ApiResponse.<AssignmentResponse>builder()
                .message("Get transaction assignment successful")
                .data(assignmentService.getAssignmentById(id))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<AssignmentResponse>>> filterAssignment(
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable){
        ApiResponse<Page<AssignmentResponse>> apiResponse = ApiResponse.<Page<AssignmentResponse>>builder()
                .message("Filter transaction assignment successful")
                .data(assignmentService.filterAssignments(pageable))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

} 