package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateModelRequest;
import com.concentrix.asset.dto.request.UpdateModelRequest;
import com.concentrix.asset.dto.response.ModelResponse;
import com.concentrix.asset.service.ModelService;
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
@RequestMapping("/model")
public class ModelController {
    ModelService modelService;

    @PostMapping
    public ResponseEntity<ApiResponse<ModelResponse>> createModel(@Valid @RequestBody CreateModelRequest request) {
        ApiResponse<ModelResponse> response = ApiResponse.<ModelResponse>builder()
                .message("Model created successfully")
                .data(modelService.createModel(request))
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ModelResponse>> getModelById(@PathVariable Integer id) {
        ApiResponse<ModelResponse> response = ApiResponse.<ModelResponse>builder()
                .message("Get model successfully")
                .data(modelService.getModelById(id))
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<ModelResponse>>> filterModel(
            @PageableDefault(size = 10, page = 0, sort = "modelId", direction = Sort.Direction.DESC) Pageable pageable) {
        ApiResponse<Page<ModelResponse>> response = ApiResponse.<Page<ModelResponse>>builder()
                .message("Get all models successfully")
                .data(modelService.filterModel(pageable))
                .build();
        return ResponseEntity.ok(response);
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<ModelResponse>> updateModel(@Valid @RequestBody UpdateModelRequest request) {
        ApiResponse<ModelResponse> response = ApiResponse.<ModelResponse>builder()
                .message("Update model successfully")
                .data(modelService.updateModel(request))
                .build();
        return ResponseEntity.ok(response);
    }
}