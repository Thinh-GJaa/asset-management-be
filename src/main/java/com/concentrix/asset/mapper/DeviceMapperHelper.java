package com.concentrix.asset.mapper;

import com.concentrix.asset.dto.response.DeviceResponse;
import com.concentrix.asset.entity.Model;
import com.concentrix.asset.entity.PODetail;
import com.concentrix.asset.repository.DeviceRepository;
import com.concentrix.asset.repository.ModelRepository;
import com.concentrix.asset.repository.PODetailRepository;
import com.concentrix.asset.repository.PORepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

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
                .build();
    }

    @Named("mapPoId")
    public String mapPoId(Integer deviceId) {
        PODetail poDetail = poDetailRepository.findByDeviceDeviceId(deviceId);
        if (poDetail == null || poDetail.getPurchaseOrder() == null)
            return null;
        return poDetail.getPurchaseOrder().getPoId();
    }

    @Named("mapPurchaseDate")
    public LocalDate mapPurchaseDate(Integer deviceId) {
        PODetail poDetail = poDetailRepository.findByDeviceDeviceId(deviceId);
        if (poDetail == null || poDetail.getPurchaseOrder() == null)
            return null;
        return poDetail.getPurchaseOrder().getCreatedAt();
    }
}
