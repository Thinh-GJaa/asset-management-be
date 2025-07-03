package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateModelRequest;
import com.concentrix.asset.dto.request.UpdateModelRequest;
import com.concentrix.asset.dto.response.ModelResponse;
import com.concentrix.asset.entity.Model;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ModelMapper {
    ModelResponse toModelResponse(Model model);

    Model toModel(CreateModelRequest request);

    void updateModel(@MappingTarget Model model, UpdateModelRequest request);
}