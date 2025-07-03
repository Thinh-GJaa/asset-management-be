package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateVendorRequest;
import com.concentrix.asset.dto.request.UpdateVendorRequest;
import com.concentrix.asset.dto.response.VendorResponse;
import com.concentrix.asset.entity.Vendor;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.VendorMapper;
import com.concentrix.asset.repository.VendorRepository;
import com.concentrix.asset.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {
    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;

    @Override
    public VendorResponse getVendorById(Integer id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.VENDOR_NOT_FOUND, id));
        return vendorMapper.toVendorResponse(vendor);
    }

    @Override
    @Transactional
    public VendorResponse createVendor(CreateVendorRequest request) {

        Vendor vendor = vendorMapper.toVendor(request);
        vendor = vendorRepository.save(vendor);
        return vendorMapper.toVendorResponse(vendor);
    }

    @Override
    @Transactional
    public VendorResponse updateVendor(UpdateVendorRequest request) {
        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new CustomException(ErrorCode.VENDOR_NOT_FOUND, request.getVendorId()));
        vendorMapper.updateVendor(vendor, request);
        vendor = vendorRepository.save(vendor);
        return vendorMapper.toVendorResponse(vendor);
    }

    @Override
    public Page<VendorResponse> filterVendor(Pageable pageable) {
        return vendorRepository.findAll(pageable).map(vendorMapper::toVendorResponse);
    }
}