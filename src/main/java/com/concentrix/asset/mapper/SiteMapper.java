package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateSiteRequest;
import com.concentrix.asset.dto.request.UpdateSiteRequest;
import com.concentrix.asset.dto.response.SiteResponse;
import com.concentrix.asset.entity.Site;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface SiteMapper {

    SiteResponse toSiteResponse(Site site);

    Site toSite(CreateSiteRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Site updateSite(@MappingTarget Site site, UpdateSiteRequest request);

    Site toSite(UpdateSiteRequest request);

    void updateSiteFromDto(UpdateSiteRequest request, @MappingTarget Site site);
}
