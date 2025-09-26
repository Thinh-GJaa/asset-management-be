package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateSiteRequest;
import com.concentrix.asset.dto.request.UpdateSiteRequest;
import com.concentrix.asset.dto.response.SiteResponse;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.SiteMapper;
import com.concentrix.asset.repository.SiteRepository;
import com.concentrix.asset.service.SiteService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class SiteServiceImpl implements SiteService {

    SiteRepository siteRepository;
    SiteMapper siteMapper;

    @Override
    public SiteResponse getSiteById(Integer id) {
        Site site = siteRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.SITE_NOT_FOUND, id));
        return siteMapper.toSiteResponse(site);
    }

    @Override
    public SiteResponse createSite(CreateSiteRequest request) {

        if (siteRepository.findBySiteName(request.getSiteName()).isPresent()) {
            throw new CustomException(ErrorCode.SITE_ALREADY_EXISTS, request.getSiteName());
        }

        Site site = siteMapper.toSite(request);
        site = siteRepository.save(site);
        return siteMapper.toSiteResponse(site);
    }

    @Override
    public SiteResponse updateSite(UpdateSiteRequest request) {
        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new CustomException(ErrorCode.SITE_NOT_FOUND, request.getSiteId()));

        // Kiểm tra tên site mới có bị trùng với site khác không
        Optional<Site> existingOpt = siteRepository.findBySiteName(request.getSiteName());
        if (existingOpt.isPresent() && !existingOpt.get().getSiteId().equals(site.getSiteId())) {
            throw new CustomException(ErrorCode.SITE_ALREADY_EXISTS, request.getSiteName());
        }

        site = siteMapper.updateSite(site, request);
        site = siteRepository.save(site);
        return siteMapper.toSiteResponse(site);
    }

    @Override
    public void deleteSite(Integer id) {
        siteRepository.deleteById(id);
    }

    @Override
    public Page<SiteResponse> filterSite(Pageable pageable) {
        Page<Site> sitePage = siteRepository.findAll(pageable);
        return sitePage.map(siteMapper::toSiteResponse);
    }
}
