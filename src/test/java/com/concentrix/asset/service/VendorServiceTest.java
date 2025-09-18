package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateVendorRequest;
import com.concentrix.asset.dto.request.UpdateVendorRequest;
import com.concentrix.asset.dto.response.VendorResponse;
import com.concentrix.asset.entity.Vendor;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.VendorMapper;
import com.concentrix.asset.repository.VendorRepository;
import com.concentrix.asset.service.impl.VendorServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock private VendorRepository vendorRepository;
    @Mock private VendorMapper vendorMapper;

    @InjectMocks private VendorServiceImpl service;

    @Test
    void getVendorById_success() {
        Vendor v = new Vendor(); v.setVendorId(1);
        VendorResponse vr = new VendorResponse(); vr.setVendorId(1);
        when(vendorRepository.findById(1)).thenReturn(Optional.of(v));
        when(vendorMapper.toVendorResponse(v)).thenReturn(vr);

        VendorResponse res = service.getVendorById(1);
        assertEquals(1, res.getVendorId());
        verify(vendorRepository).findById(1);
        verify(vendorMapper).toVendorResponse(v);
    }

    @Test
    void getVendorById_notFound_throws() {
        when(vendorRepository.findById(99)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.getVendorById(99));
        assertEquals(ErrorCode.VENDOR_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createVendor_mapsAndSaves() {
        CreateVendorRequest req = new CreateVendorRequest();
        Vendor v = new Vendor();
        VendorResponse vr = new VendorResponse();
        when(vendorMapper.toVendor(req)).thenReturn(v);
        when(vendorRepository.save(v)).thenReturn(v);
        when(vendorMapper.toVendorResponse(v)).thenReturn(vr);

        VendorResponse res = service.createVendor(req);
        assertNotNull(res);
        verify(vendorMapper).toVendor(req);
        verify(vendorRepository).save(v);
        verify(vendorMapper).toVendorResponse(v);
    }

    @Test
    void updateVendor_notFound_throws() {
        UpdateVendorRequest req = new UpdateVendorRequest(); req.setVendorId(5);
        when(vendorRepository.findById(5)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.updateVendor(req));
        assertEquals(ErrorCode.VENDOR_NOT_FOUND, ex.getErrorCode());
        verify(vendorMapper, never()).updateVendor(any(), any());
    }

    @Test
    void updateVendor_updatesAndSaves() {
        UpdateVendorRequest req = new UpdateVendorRequest(); req.setVendorId(2);
        Vendor v = new Vendor(); v.setVendorId(2);
        VendorResponse vr = new VendorResponse(); vr.setVendorId(2);
        when(vendorRepository.findById(2)).thenReturn(Optional.of(v));
        // mapper updates entity in place
        when(vendorRepository.save(v)).thenReturn(v);
        when(vendorMapper.toVendorResponse(v)).thenReturn(vr);

        VendorResponse res = service.updateVendor(req);
        assertEquals(2, res.getVendorId());
        verify(vendorMapper).updateVendor(v, req);
        verify(vendorRepository).save(v);
    }

    @Test
    void filterVendor_noSearch_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Vendor v = new Vendor(); VendorResponse vr = new VendorResponse();
        Page<Vendor> page = new PageImpl<>(List.of(v), pageable, 1);
        when(vendorRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Vendor>>any(), eq(pageable))).thenReturn(page);
        when(vendorMapper.toVendorResponse(v)).thenReturn(vr);

        Page<VendorResponse> res = service.filterVendor(pageable, null);
        assertEquals(1, res.getTotalElements());
        verify(vendorRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Vendor>>any(), eq(pageable));
    }

    @Test
    void filterVendor_withSearch_buildsSpecAndMaps() {
        Pageable pageable = PageRequest.of(0, 10);
        Vendor v = new Vendor(); VendorResponse vr = new VendorResponse();
        Page<Vendor> page = new PageImpl<>(List.of(v), pageable, 1);
        when(vendorRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Vendor>>any(), eq(pageable))).thenReturn(page);
        when(vendorMapper.toVendorResponse(v)).thenReturn(vr);

        Page<VendorResponse> res = service.filterVendor(pageable, "   Abc  ");
        assertEquals(1, res.getTotalElements());
        verify(vendorRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Vendor>>any(), eq(pageable));
    }
}


