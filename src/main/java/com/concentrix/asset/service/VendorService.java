package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateVendorRequest;
import com.concentrix.asset.dto.request.UpdateVendorRequest;
import com.concentrix.asset.dto.response.VendorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VendorService {
    VendorResponse getVendorById(Integer id);

    VendorResponse createVendor(CreateVendorRequest request);

    VendorResponse updateVendor(UpdateVendorRequest request);

    Page<VendorResponse> filterVendor(Pageable pageable);
}