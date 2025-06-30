package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.request.CreateSiteRequest;
import com.concentrix.asset.dto.response.SiteResponse;
import com.concentrix.asset.entity.Site;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SiteMapper {

    SiteResponse toSiteResponse(Site site);


    Site toSite(CreateSiteRequest request);
}
