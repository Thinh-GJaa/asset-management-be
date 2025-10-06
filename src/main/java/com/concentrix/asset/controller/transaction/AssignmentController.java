package com.concentrix.asset.controller.transaction;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateAssignmentRequest;
import com.concentrix.asset.dto.request.LaptopBadgeRequest;
import com.concentrix.asset.dto.response.AssetHandoverResponse;
import com.concentrix.asset.dto.response.AssignmentResponse;
import com.concentrix.asset.service.transaction.AssignmentService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/transaction/assignment")
@Slf4j
public class AssignmentController {

    AssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request) {
        ApiResponse<AssignmentResponse> apiResponse = ApiResponse.<AssignmentResponse>builder()
                .message("Transaction assignment created successful")
                .data(assignmentService.createAssignment(request))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getById(
            @PathVariable Integer id) {
        ApiResponse<AssignmentResponse> apiResponse = ApiResponse.<AssignmentResponse>builder()
                .message("Get transaction assignment successful")
                .data(assignmentService.getAssignmentById(id))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<AssignmentResponse>>> filterAssignment(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate toDate,
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ApiResponse<Page<AssignmentResponse>> apiResponse = ApiResponse.<Page<AssignmentResponse>>builder()
                .message("Filter transaction assignment successful")
                .data(assignmentService.filterAssignments(search, fromDate, toDate, pageable))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{assignmentId}/handover-form")
    public ResponseEntity<AssetHandoverResponse> getAssetHandoverByAssignmentId(@PathVariable Integer assignmentId) {
        AssetHandoverResponse response = assignmentService.getAssetHandoverByAssignmentId(assignmentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/request-laptop-badge")
    public ResponseEntity<ApiResponse<Void>> requestLaptopBadge(@Valid @RequestBody LaptopBadgeRequest request)
            throws MessagingException {
        assignmentService.requestLaptopBadge(request);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .message("Request laptop badge successful")
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{assignmentId}/upload-image")
    public ResponseEntity<ApiResponse<Void>> uploadImage(
            @PathVariable Integer assignmentId,
            @RequestParam("images") List<MultipartFile> images,
            HttpServletRequest request) {
        assignmentService.uploadImage(assignmentId, images);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .message("Upload image successful")
                .build();
        return ResponseEntity.ok(response);
    }

}