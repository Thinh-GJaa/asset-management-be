package com.concentrix.asset.service.impl;


import com.concentrix.asset.dto.request.CreateSiteRequest;
import com.concentrix.asset.dto.response.SiteResponse;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.mapper.SiteMapper;
import com.concentrix.asset.repository.SiteRepository;
import com.concentrix.asset.service.SiteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {

    SiteRepository siteRepository;
    SiteMapper siteMapper;

    @Override
    public Site getSiteById(Long id) {
        return null;
    }

    @Override
    public SiteResponse createSite(CreateSiteRequest request) {
        Site site = siteMapper.toSite(request);
        site = siteRepository.save(site);

        return siteMapper.toSiteResponse(site);
    }

    @Override
    public Site updateSite(Long id, Site site) {
        return null;
    }

    @Override
    public void deleteSite(Long id) {

    }
}
