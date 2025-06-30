package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateSiteRequest;
import com.concentrix.asset.dto.response.SiteResponse;
import com.concentrix.asset.entity.Site;
import org.springframework.data.domain.Pageable;

public interface SiteService {

     Site getSiteById(Long id);

     SiteResponse createSite(CreateSiteRequest request);

     Site updateSite(Long id, Site site);

     void deleteSite(Long id);

}
