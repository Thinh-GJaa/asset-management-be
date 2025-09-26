package com.concentrix.asset.mapper.helper;

import com.concentrix.asset.dto.response.DeviceResponse;
import com.concentrix.asset.dto.response.SiteResponse;
import com.concentrix.asset.entity.Device;
import com.concentrix.asset.entity.Model;
import com.concentrix.asset.entity.PODetail;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.repository.DeviceRepository;
import com.concentrix.asset.repository.ModelRepository;
import com.concentrix.asset.repository.PODetailRepository;
import com.concentrix.asset.repository.PORepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class DeviceMapperHelper {

    DeviceRepository deviceRepository;
    ModelRepository modelRepository;
    PORepository poRepository;
    PODetailRepository poDetailRepository;

    @Named("mapModel")
    public DeviceResponse.ModelResponse mapModel(Model model) {
        if (model == null) {
            return null;
        }
        return DeviceResponse.ModelResponse.builder()
                .modelId(model.getModelId())
                .modelName(model.getModelName())
                .type(model.getType())
                .build();
    }

    @Named("mapPoId")
    public String mapPoId(List<PODetail> poDetails) {
        if (poDetails == null || poDetails.isEmpty())
            return null;
        return poDetails.get(0).getPurchaseOrder().getPoId();
    }

    @Named("mapPurchaseDate")
    public LocalDate mapPurchaseDate(List<PODetail> poDetails) {
        if (poDetails == null || poDetails.isEmpty())
            return null;
        return poDetails.get(0).getPurchaseOrder().getCreatedAt();
    }

    @Named("mapSite")
    public SiteResponse mapSite(Device device) {
        if (device == null)
            return null;
        Site site = null;
        if (device.getCurrentWarehouse() != null && device.getCurrentWarehouse().getSite() != null) {
            site = device.getCurrentWarehouse().getSite();
        } else if (device.getCurrentFloor() != null && device.getCurrentFloor().getSite() != null) {
            site = device.getCurrentFloor().getSite();
        }
        if (site == null)
            return null;
        return SiteResponse.builder()
                .siteId(site.getSiteId() != null ? site.getSiteId().longValue() : null)
                .siteName(site.getSiteName())
                .build();
    }
}
