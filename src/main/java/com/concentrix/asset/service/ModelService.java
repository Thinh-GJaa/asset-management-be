package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateModelRequest;
import com.concentrix.asset.dto.request.UpdateModelRequest;
import com.concentrix.asset.dto.response.ModelResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import com.concentrix.asset.enums.DeviceType;

public interface ModelService {
    ModelResponse getModelById(Integer id);

    ModelResponse createModel(CreateModelRequest request);

    ModelResponse updateModel(UpdateModelRequest request);

    Page<ModelResponse> filterModel(Pageable pageable);

    // Overloaded: support optional search and type filters
    Page<ModelResponse> filterModel(Pageable pageable, String search, DeviceType type);

    List<ModelResponse> getModelsByType(DeviceType type);

    List<ModelResponse> getModelWithSerial();

    List<ModelResponse> getModelWithoutSerial();
}