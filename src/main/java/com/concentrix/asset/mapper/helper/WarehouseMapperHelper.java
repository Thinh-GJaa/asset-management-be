package com.concentrix.asset.mapper.helper;

import com.concentrix.asset.entity.Site;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class WarehouseMapperHelper {


    SiteRepository siteRepository;

    @Named("siteIdToSite")
    public Site siteIdToSite(Integer siteId) {
        return siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.SITE_NOT_FOUND, siteId));
    }
}
