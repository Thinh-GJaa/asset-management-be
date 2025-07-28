package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateSiteRequest;
import com.concentrix.asset.dto.request.UpdateSiteRequest;
import com.concentrix.asset.dto.response.SiteResponse;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.SiteMapper;
import com.concentrix.asset.repository.SiteRepository;
import com.concentrix.asset.service.SiteService;
import com.concentrix.asset.service.TypeService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class TypeServiceImpl implements TypeService {


    @Override
    public List<DeviceType> getTypes() {
        return Arrays.stream(DeviceType.values())
                .toList();
    }

    @Override
    public List<DeviceType> getTypeWithSerial() {
        return Arrays.stream(DeviceType.values())
                .filter(DeviceType::hasSerial)
                .toList();
    }

    @Override
    public List<DeviceType> getTypeWithoutSerial() {
        return Arrays.stream(DeviceType.values())
                .filter(type -> !type.hasSerial())
                .toList();
    }

}
