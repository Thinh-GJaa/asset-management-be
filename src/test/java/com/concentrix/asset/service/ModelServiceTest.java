package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateModelRequest;
import com.concentrix.asset.dto.request.UpdateModelRequest;
import com.concentrix.asset.dto.response.ModelResponse;
import com.concentrix.asset.entity.Model;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.ModelMapper;
import com.concentrix.asset.repository.ModelRepository;
import com.concentrix.asset.service.impl.ModelServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelServiceTest {

    @Mock private ModelRepository modelRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private TypeService typeService;

    @InjectMocks private ModelServiceImpl service;

    @Test
    void getModelById_found_and_notFound() {
        Model model = new Model(); model.setModelId(1); model.setModelName("M1");
        ModelResponse resp = new ModelResponse(); resp.setModelId(1); resp.setModelName("M1");
        when(modelRepository.findById(1)).thenReturn(Optional.of(model));
        when(modelMapper.toModelResponse(model)).thenReturn(resp);
        assertEquals(1, service.getModelById(1).getModelId());

        when(modelRepository.findById(404)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.getModelById(404));
        assertEquals(ErrorCode.MODEL_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createModel_duplicate_and_success() {
        CreateModelRequest req = new CreateModelRequest();
        req.setModelName("Dup");

        when(modelRepository.findByModelName("Dup")).thenReturn(Optional.of(new Model()));
        CustomException ex = assertThrows(CustomException.class, () -> service.createModel(req));
        assertEquals(ErrorCode.MODEL_ALREADY_EXISTS, ex.getErrorCode());

        // success path
        Model entity = new Model(); entity.setModelId(10); entity.setModelName("New");
        ModelResponse resp = new ModelResponse(); resp.setModelId(10); resp.setModelName("New");
        CreateModelRequest req2 = new CreateModelRequest(); req2.setModelName("New");
        when(modelRepository.findByModelName("New")).thenReturn(Optional.empty());
        when(modelMapper.toModel(req2)).thenReturn(entity);
        when(modelRepository.save(entity)).thenReturn(entity);
        when(modelMapper.toModelResponse(entity)).thenReturn(resp);
        ModelResponse got = service.createModel(req2);
        assertEquals(10, got.getModelId());
    }

    @Test
    void updateModel_notFound_duplicate_and_success() {
        UpdateModelRequest req = new UpdateModelRequest();
        req.setModelId(1);
        req.setModelName("M1");

        when(modelRepository.findById(1)).thenReturn(Optional.empty());
        CustomException nf = assertThrows(CustomException.class, () -> service.updateModel(req));
        assertEquals(ErrorCode.MODEL_NOT_FOUND, nf.getErrorCode());

        // duplicate name on other id
        Model existing = new Model(); existing.setModelId(1); existing.setModelName("Old");
        when(modelRepository.findById(1)).thenReturn(Optional.of(existing));
        Model other = new Model(); other.setModelId(2); other.setModelName("M1");
        when(modelRepository.findByModelName("M1")).thenReturn(Optional.of(other));
        CustomException dup = assertThrows(CustomException.class, () -> service.updateModel(req));
        assertEquals(ErrorCode.MODEL_ALREADY_EXISTS, dup.getErrorCode());

        // success: same id when searching by name -> allowed
        when(modelRepository.findByModelName("M1")).thenReturn(Optional.of(existing));
        doAnswer(inv -> null).when(modelMapper).updateModel(existing, req);
        when(modelRepository.save(existing)).thenReturn(existing);
        ModelResponse updatedResp = new ModelResponse(); updatedResp.setModelId(1); updatedResp.setModelName("M1");
        when(modelMapper.toModelResponse(existing)).thenReturn(updatedResp);
        ModelResponse ok = service.updateModel(req);
        assertEquals(1, ok.getModelId());
    }

    @Test
    void filterModel_simple_pageable_mapping() {
        Pageable pageable = PageRequest.of(0, 5);
        Model m = new Model(); m.setModelId(1);
        when(modelRepository.findAll(pageable)).thenReturn(new PageImpl<>(Collections.singletonList(m), pageable, 1));
        ModelResponse mapped = new ModelResponse(); mapped.setModelId(1);
        when(modelMapper.toModelResponse(m)).thenReturn(mapped);
        Page<ModelResponse> page = service.filterModel(pageable);
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void filterModel_with_search_and_type_specification() {
        Pageable pageable = PageRequest.of(0, 10);
        when(modelRepository.findAll(Mockito.<Specification<Model>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(Collections.singletonList(new Model()), pageable, 1));
        ModelResponse specResp = new ModelResponse(); specResp.setModelId(1);
        when(modelMapper.toModelResponse(any(Model.class))).thenReturn(specResp);
        Page<ModelResponse> page = service.filterModel(pageable, "search", DeviceType.LAPTOP);
        assertEquals(1, page.getTotalElements());
        verify(modelRepository).findAll(Mockito.<Specification<Model>>any(), eq(pageable));
    }

    @Test
    void getModelsByType_maps_list() {
        Model m1 = new Model(); m1.setModelId(1);
        Model m2 = new Model(); m2.setModelId(2);
        when(modelRepository.findByType(DeviceType.MOUSE)).thenReturn(Arrays.asList(m1, m2));
        ModelResponse r1 = new ModelResponse(); r1.setModelId(1);
        ModelResponse r2 = new ModelResponse(); r2.setModelId(2);
        when(modelMapper.toModelResponse(m1)).thenReturn(r1);
        when(modelMapper.toModelResponse(m2)).thenReturn(r2);
        List<ModelResponse> list = service.getModelsByType(DeviceType.MOUSE);
        assertEquals(2, list.size());
    }

    @Test
    void getModelWithSerial_aggregates() {
        when(typeService.getTypeWithSerial()).thenReturn(List.of(DeviceType.LAPTOP, DeviceType.MONITOR));
        Model m1 = new Model(); Model m2 = new Model();
        when(modelRepository.findByType(DeviceType.LAPTOP)).thenReturn(List.of(m1));
        when(modelRepository.findByType(DeviceType.MONITOR)).thenReturn(List.of(m2));
        ModelResponse mr1 = new ModelResponse(); mr1.setModelId(1);
        ModelResponse mr2 = new ModelResponse(); mr2.setModelId(2);
        when(modelMapper.toModelResponse(m1)).thenReturn(mr1);
        when(modelMapper.toModelResponse(m2)).thenReturn(mr2);
        List<ModelResponse> list = service.getModelWithSerial();
        assertEquals(2, list.size());
    }

    @Test
    void getModelWithoutSerial_aggregates() {
        when(typeService.getTypeWithoutSerial()).thenReturn(List.of(DeviceType.MOUSE, DeviceType.KEYBOARD));
        Model m1 = new Model(); Model m2 = new Model();
        when(modelRepository.findByType(DeviceType.MOUSE)).thenReturn(List.of(m1));
        when(modelRepository.findByType(DeviceType.KEYBOARD)).thenReturn(List.of(m2));
        ModelResponse nr1 = new ModelResponse(); nr1.setModelId(1);
        ModelResponse nr2 = new ModelResponse(); nr2.setModelId(2);
        when(modelMapper.toModelResponse(m1)).thenReturn(nr1);
        when(modelMapper.toModelResponse(m2)).thenReturn(nr2);
        List<ModelResponse> list = service.getModelWithoutSerial();
        assertEquals(2, list.size());
    }
}


