package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateSiteRequest;
import com.concentrix.asset.dto.request.UpdateSiteRequest;
import com.concentrix.asset.dto.response.SiteResponse;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.SiteMapper;
import com.concentrix.asset.repository.SiteRepository;
import com.concentrix.asset.service.impl.SiteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiteServiceTest {

    @Mock private SiteRepository siteRepository;
    @Mock private SiteMapper siteMapper;

    @InjectMocks private SiteServiceImpl service;

    private Site site;
    private SiteResponse siteResponse;
    private CreateSiteRequest createReq;
    private UpdateSiteRequest updateReq;

    @BeforeEach
    void setUp() {
        site = new Site();
        site.setSiteId(1);
        site.setSiteName("S1");
        site.setSiteLocation("HCM");

        siteResponse = new SiteResponse();
        siteResponse.setSiteId(1L);
        siteResponse.setSiteName("S1");

        createReq = CreateSiteRequest.builder()
                .siteName("S1")
                .siteLocation("HCM")
                .build();

        updateReq = UpdateSiteRequest.builder()
                .siteId(1)
                .siteName("S1-new")
                .siteLocation("HN")
                .build();
    }

    @Test
    void getSiteById_success() {
        when(siteRepository.findById(1)).thenReturn(Optional.of(site));
        when(siteMapper.toSiteResponse(site)).thenReturn(siteResponse);

        SiteResponse res = service.getSiteById(1);

        assertNotNull(res);
        assertEquals(1L, res.getSiteId());
        verify(siteRepository).findById(1);
        verify(siteMapper).toSiteResponse(site);
    }

    @Test
    void getSiteById_notFound_throws() {
        when(siteRepository.findById(99)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> service.getSiteById(99));
        assertEquals(ErrorCode.SITE_NOT_FOUND, ex.getErrorCode());
        verify(siteMapper, never()).toSiteResponse(any());
    }

    @Test
    void createSite_duplicateName_throws() {
        when(siteRepository.findBySiteName("S1")).thenReturn(Optional.of(site));

        CustomException ex = assertThrows(CustomException.class, () -> service.createSite(createReq));
        assertEquals(ErrorCode.SITE_ALREADY_EXISTS, ex.getErrorCode());
        verify(siteRepository, never()).save(any());
    }

    @Test
    void createSite_success_mapsAndSaves() {
        when(siteRepository.findBySiteName("S1")).thenReturn(Optional.empty());
        when(siteMapper.toSite(createReq)).thenReturn(site);
        when(siteRepository.save(site)).thenReturn(site);
        when(siteMapper.toSiteResponse(site)).thenReturn(siteResponse);

        SiteResponse res = service.createSite(createReq);

        assertNotNull(res);
        assertEquals("S1", res.getSiteName());
        verify(siteRepository).findBySiteName("S1");
        verify(siteRepository).save(site);
        verify(siteMapper).toSite(createReq);
        verify(siteMapper).toSiteResponse(site);
    }

    @Test
    void updateSite_notFound_throws() {
        when(siteRepository.findById(1)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> service.updateSite(updateReq));
        assertEquals(ErrorCode.SITE_NOT_FOUND, ex.getErrorCode());
        verify(siteRepository, never()).save(any());
    }

    @Test
    void updateSite_duplicateNewName_throws() {
        Site existing = new Site(); existing.setSiteId(2); existing.setSiteName("S1-new");
        when(siteRepository.findById(1)).thenReturn(Optional.of(site));
        when(siteRepository.findBySiteName("S1-new")).thenReturn(Optional.of(existing));

        CustomException ex = assertThrows(CustomException.class, () -> service.updateSite(updateReq));
        assertEquals(ErrorCode.SITE_ALREADY_EXISTS, ex.getErrorCode());
        verify(siteRepository, never()).save(any());
    }

    @Test
    void updateSite_success_updatesAndSaves() {
        when(siteRepository.findById(1)).thenReturn(Optional.of(site));
        when(siteRepository.findBySiteName("S1-new")).thenReturn(Optional.empty());
        when(siteMapper.updateSite(site, updateReq)).thenReturn(site);
        when(siteRepository.save(site)).thenReturn(site);
        when(siteMapper.toSiteResponse(site)).thenReturn(siteResponse);

        SiteResponse res = service.updateSite(updateReq);

        assertNotNull(res);
        verify(siteMapper).updateSite(site, updateReq);
        verify(siteRepository).save(site);
        verify(siteMapper).toSiteResponse(site);
    }

    @Test
    void deleteSite_callsRepository() {
        assertDoesNotThrow(() -> service.deleteSite(5));
        verify(siteRepository).deleteById(5);
    }

    @Test
    void filterSite_mapsPageContent() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Site> page = new PageImpl<>(List.of(site), pageable, 1);
        when(siteRepository.findAll(pageable)).thenReturn(page);
        when(siteMapper.toSiteResponse(site)).thenReturn(siteResponse);

        Page<SiteResponse> res = service.filterSite(pageable);

        assertEquals(1, res.getTotalElements());
        assertEquals(1L, res.getContent().get(0).getSiteId());
        verify(siteRepository).findAll(pageable);
        verify(siteMapper).toSiteResponse(site);
    }
}


