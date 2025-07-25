package com.concentrix.asset.dto.response;

import com.concentrix.asset.enums.DeviceType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TypeSummaryResponse {
    DeviceType type;
    int total;
    List<ModelSummaryResponse> models;
}