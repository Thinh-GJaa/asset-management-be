package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateWarehouseRequest;
import com.concentrix.asset.dto.request.UpdateWarehouseRequest;
import com.concentrix.asset.dto.response.WarehouseResponse;
import com.concentrix.asset.entity.Warehouse;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.WarehouseMapper;
import com.concentrix.asset.repository.WarehouseRepository;
import com.concentrix.asset.service.impl.WarehouseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock private WarehouseRepository warehouseRepository;
    @Mock private WarehouseMapper warehouseMapper;

    @InjectMocks private WarehouseServiceImpl service;

    @Test
    void getWarehouseById_success() {
        Warehouse w = new Warehouse(); w.setWarehouseId(1);
        WarehouseResponse wr = new WarehouseResponse(); wr.setWarehouseId(1);
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(w));
        when(warehouseMapper.toWarehouseResponse(w)).thenReturn(wr);

        WarehouseResponse res = service.getWarehouseById(1);
        assertEquals(1, res.getWarehouseId());
        verify(warehouseRepository).findById(1);
        verify(warehouseMapper).toWarehouseResponse(w);
    }

    @Test
    void getWarehouseById_notFound_throws() {
        when(warehouseRepository.findById(99)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.getWarehouseById(99));
        assertEquals(ErrorCode.WAREHOUSE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createWarehouse_mapsAndSaves() {
        CreateWarehouseRequest req = new CreateWarehouseRequest();
        Warehouse w = new Warehouse();
        WarehouseResponse wr = new WarehouseResponse();
        when(warehouseMapper.toWarehouse(req)).thenReturn(w);
        when(warehouseRepository.save(w)).thenReturn(w);
        when(warehouseMapper.toWarehouseResponse(w)).thenReturn(wr);

        WarehouseResponse res = service.createWarehouse(req);
        assertNotNull(res);
        verify(warehouseMapper).toWarehouse(req);
        verify(warehouseRepository).save(w);
        verify(warehouseMapper).toWarehouseResponse(w);
    }

    @Test
    void updateWarehouse_notFound_throws() {
        UpdateWarehouseRequest req = new UpdateWarehouseRequest(); req.setWarehouseId(7);
        when(warehouseRepository.findById(7)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.updateWarehouse(req));
        assertEquals(ErrorCode.WAREHOUSE_NOT_FOUND, ex.getErrorCode());
        verify(warehouseMapper, never()).updateWarehouse(any(), any());
    }

    @Test
    void updateWarehouse_updatesAndSaves() {
        UpdateWarehouseRequest req = new UpdateWarehouseRequest(); req.setWarehouseId(2);
        Warehouse w = new Warehouse(); w.setWarehouseId(2);
        WarehouseResponse wr = new WarehouseResponse(); wr.setWarehouseId(2);
        when(warehouseRepository.findById(2)).thenReturn(Optional.of(w));
        when(warehouseRepository.save(w)).thenReturn(w);
        when(warehouseMapper.toWarehouseResponse(w)).thenReturn(wr);

        WarehouseResponse res = service.updateWarehouse(req);
        assertEquals(2, res.getWarehouseId());
        verify(warehouseMapper).updateWarehouse(w, req);
        verify(warehouseRepository).save(w);
    }

    @Test
    void filterWarehouse_noFilters_mapsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Warehouse w = new Warehouse(); WarehouseResponse wr = new WarehouseResponse();
        Page<Warehouse> page = new PageImpl<>(List.of(w), pageable, 1);
        when(warehouseRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Warehouse>>any(), eq(pageable)))
                .thenReturn(page);
        when(warehouseMapper.toWarehouseResponse(w)).thenReturn(wr);

        Page<WarehouseResponse> res = service.filterWarehouse(pageable, null, null);
        assertEquals(1, res.getTotalElements());
        verify(warehouseRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Warehouse>>any(), eq(pageable));
    }

    @Test
    void filterWarehouse_withSearchOnly_buildsSpec() {
        Pageable pageable = PageRequest.of(0, 10);
        Warehouse w = new Warehouse(); WarehouseResponse wr = new WarehouseResponse();
        Page<Warehouse> page = new PageImpl<>(List.of(w), pageable, 1);
        when(warehouseRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Warehouse>>any(), eq(pageable)))
                .thenReturn(page);
        when(warehouseMapper.toWarehouseResponse(w)).thenReturn(wr);

        Page<WarehouseResponse> res = service.filterWarehouse(pageable, "  abc  ", null);
        assertEquals(1, res.getTotalElements());
        verify(warehouseRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Warehouse>>any(), eq(pageable));
    }

    @Test
    void filterWarehouse_withSiteOnly_buildsSpec() {
        Pageable pageable = PageRequest.of(0, 10);
        Warehouse w = new Warehouse(); WarehouseResponse wr = new WarehouseResponse();
        Page<Warehouse> page = new PageImpl<>(List.of(w), pageable, 1);
        when(warehouseRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Warehouse>>any(), eq(pageable)))
                .thenReturn(page);
        when(warehouseMapper.toWarehouseResponse(w)).thenReturn(wr);

        Page<WarehouseResponse> res = service.filterWarehouse(pageable, null, 3);
        assertEquals(1, res.getTotalElements());
        verify(warehouseRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Warehouse>>any(), eq(pageable));
    }

    @Test
    void filterWarehouse_withSearchAndSite_buildsSpec() {
        Pageable pageable = PageRequest.of(0, 10);
        Warehouse w = new Warehouse(); WarehouseResponse wr = new WarehouseResponse();
        Page<Warehouse> page = new PageImpl<>(List.of(w), pageable, 1);
        when(warehouseRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Warehouse>>any(), eq(pageable)))
                .thenReturn(page);
        when(warehouseMapper.toWarehouseResponse(w)).thenReturn(wr);

        Page<WarehouseResponse> res = service.filterWarehouse(pageable, "abc", 3);
        assertEquals(1, res.getTotalElements());
        verify(warehouseRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Warehouse>>any(), eq(pageable));
    }
}


