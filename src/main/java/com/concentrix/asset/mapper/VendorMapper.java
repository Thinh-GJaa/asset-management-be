package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateVendorRequest;
import com.concentrix.asset.dto.request.UpdateVendorRequest;
import com.concentrix.asset.dto.response.VendorResponse;
import com.concentrix.asset.entity.Vendor;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VendorMapper {
    VendorResponse toVendorResponse(Vendor vendor);

    Vendor toVendor(CreateVendorRequest request);

    void updateVendor(@MappingTarget Vendor vendor, UpdateVendorRequest request);
}