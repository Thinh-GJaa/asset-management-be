package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateFloorRequest;
import com.concentrix.asset.dto.request.UpdateFloorRequest;
import com.concentrix.asset.dto.response.FloorResponse;
import com.concentrix.asset.entity.Floor;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.mapper.FloorMapper;
import com.concentrix.asset.repository.FloorRepository;
import com.concentrix.asset.repository.SiteRepository;
import com.concentrix.asset.service.impl.FloorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class FloorServiceTest {
    @Mock
    private FloorRepository floorRepository;
    @Mock
    private FloorMapper floorMapper;
    @Mock
    private SiteRepository siteRepository;

    @InjectMocks
    private FloorServiceImpl floorService;

    private Floor floor;
    private FloorResponse floorResponse;
    private CreateFloorRequest createRequest;
    private UpdateFloorRequest updateRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        floor = new Floor();
        floor.setFloorId(1);
        floorResponse = new FloorResponse();
        createRequest = new CreateFloorRequest();
        updateRequest = new UpdateFloorRequest();
        updateRequest.setFloorId(1);
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void givenValidFloorId_whenGetFloorById_thenReturnFloorResponse() {
        when(floorRepository.findById(1)).thenReturn(Optional.of(floor));
        when(floorMapper.toFloorResponse(floor)).thenReturn(floorResponse);

        FloorResponse result = floorService.getFloorById(1);

        assertNotNull(result);
        verify(floorRepository).findById(1);
        verify(floorMapper).toFloorResponse(floor);
    }

    @Test
    void getFloorById_ShouldThrowException_WhenFloorNotFound() {
        when(floorRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(CustomException.class, () -> floorService.getFloorById(1));
    }

    @Test
    void givenCreateRequest_whenCreateFloor_thenReturnNewFloorResponse() {
        when(floorMapper.toFloor(createRequest)).thenReturn(floor);
        when(floorRepository.save(floor)).thenReturn(floor);
        when(floorMapper.toFloorResponse(floor)).thenReturn(floorResponse);

        FloorResponse result = floorService.createFloor(createRequest);

        assertNotNull(result);
        verify(floorMapper).toFloor(createRequest);
        verify(floorRepository).save(floor);
    }

    @Test
    void givenUpdateRequest_whenUpdateFloor_thenReturnUpdatedFloorResponse() {
        when(floorRepository.findById(1)).thenReturn(Optional.of(floor));
        when(floorRepository.save(floor)).thenReturn(floor);
        when(floorMapper.toFloorResponse(floor)).thenReturn(floorResponse);

        FloorResponse result = floorService.updateFloor(updateRequest);

        assertNotNull(result);
        verify(floorMapper).updateFloor(floor, updateRequest);
        verify(floorRepository).save(floor);
    }

    @Test
    void updateFloor_ShouldThrowException_WhenFloorNotFound() {
        when(floorRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(CustomException.class, () -> floorService.updateFloor(updateRequest));
    }

    @Test
    void givenSearchCriteria_whenFilterFloor_thenReturnPagedResponse() {
        Page<Floor> floorPage = new PageImpl<>(List.of(floor));
        when(floorRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(floorPage);
        when(floorMapper.toFloorResponse(floor)).thenReturn(floorResponse);

        Page<FloorResponse> result = floorService.filterFloor("test", 1, 1, pageable);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void givenValidSiteId_whenGetFloorsBySiteId_thenReturnPagedResponse() {
        when(siteRepository.findById(1)).thenReturn(Optional.of(new Site()));
        Page<Floor> floorPage = new PageImpl<>(List.of(floor));
        when(floorRepository.findAllBySite_SiteId(1, pageable)).thenReturn(floorPage);
        when(floorMapper.toFloorResponse(floor)).thenReturn(floorResponse);

        Page<FloorResponse> result = floorService.getFloorsBySiteId(1, pageable);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void getFloorsBySiteId_ShouldThrowException_WhenSiteNotFound() {
        when(siteRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(CustomException.class, () -> floorService.getFloorsBySiteId(1, pageable));
    }
}