package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateModelRequest;
import com.concentrix.asset.dto.request.UpdateModelRequest;
import com.concentrix.asset.dto.response.ModelResponse;
import com.concentrix.asset.entity.Model;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.ModelMapper;
import com.concentrix.asset.repository.ModelRepository;
import com.concentrix.asset.service.ModelService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ModelServiceImpl implements ModelService {

    ModelRepository modelRepository;
    ModelMapper modelMapper;

    @Override
    public ModelResponse getModelById(Integer id) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MODEL_NOT_FOUND, id));
        return modelMapper.toModelResponse(model);
    }

    @Override
    @Transactional
    public ModelResponse createModel(CreateModelRequest request) {
        if (modelRepository.findByModelName(request.getModelName()).isPresent()) {
            throw new CustomException(ErrorCode.MODEL_ALREADY_EXISTS, request.getModelName());
        }
        Model model = modelMapper.toModel(request);
        model = modelRepository.save(model);
        return modelMapper.toModelResponse(model);
    }

    @Override
    @Transactional
    public ModelResponse updateModel(UpdateModelRequest request) {
        Model model = modelRepository.findById(request.getModelId())
                .orElseThrow(() -> new CustomException(ErrorCode.MODEL_NOT_FOUND, request.getModelId()));


        Optional<Model> existingOpt = modelRepository.findByModelName(request.getModelName());
        if (existingOpt.isPresent() && !existingOpt.get().getModelId().equals(model.getModelId())) {
            throw new CustomException(ErrorCode.MODEL_ALREADY_EXISTS, request.getModelName());
        }
        
        modelMapper.updateModel(model, request);
        model = modelRepository.save(model);
        return modelMapper.toModelResponse(model);
    }

    @Override
    public Page<ModelResponse> filterModel(Pageable pageable) {
        return modelRepository.findAll(pageable).map(modelMapper::toModelResponse);
    }
}