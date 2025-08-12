package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateModelRequest;
import com.concentrix.asset.dto.request.UpdateModelRequest;
import com.concentrix.asset.dto.response.ModelResponse;
import com.concentrix.asset.enums.DeviceType;
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

import java.util.List;

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
            @PageableDefault(size = 10, page = 0, sort = "modelId", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "type", required = false) DeviceType type) {
        ApiResponse<Page<ModelResponse>> response = ApiResponse.<Page<ModelResponse>>builder()
                .message("Get all models successfully")
                .data(modelService.filterModel(pageable, search, type))
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/type")
    public ResponseEntity<ApiResponse<List<ModelResponse>>> getModelsByType(@RequestParam("type") DeviceType type) {
        ApiResponse<List<ModelResponse>> response = ApiResponse.<List<ModelResponse>>builder()
                .message("Get models by type successfully")
                .data(modelService.getModelsByType(type))
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

    @GetMapping("/with-serial")
    public ResponseEntity<ApiResponse<List<ModelResponse>>> getModelWithSerial() {
        ApiResponse<List<ModelResponse>> response = ApiResponse.<List<ModelResponse>>builder()
                .message("Get models with serial successfully")
                .data(modelService.getModelWithSerial())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/without-serial")
    public ResponseEntity<ApiResponse<List<ModelResponse>>> getModelWithoutSerial() {
        ApiResponse<List<ModelResponse>> response = ApiResponse.<List<ModelResponse>>builder()
                .message("Get models without serial successfully")
                .data(modelService.getModelWithoutSerial())
                .build();
        return ResponseEntity.ok(response);
    }


}