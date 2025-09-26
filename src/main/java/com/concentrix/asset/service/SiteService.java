package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateSiteRequest;
import com.concentrix.asset.dto.request.UpdateSiteRequest;
import com.concentrix.asset.dto.response.SiteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SiteService {

    SiteResponse getSiteById(Integer id);

    SiteResponse createSite(CreateSiteRequest request);

    SiteResponse updateSite(UpdateSiteRequest request);

    void deleteSite(Integer id);

    Page<SiteResponse> filterSite(Pageable pageable);

}
