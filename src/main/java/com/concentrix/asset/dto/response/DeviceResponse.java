package com.concentrix.asset.dto.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceResponse implements Serializable{

    String deviceId;
    String serialNumber;
    String deviceName;
    String poId;
    LocalDate purchaseDate;

    ModelResponse model;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ModelResponse implements Serializable{
        Integer modelId;
        String modelName;
    }

}
