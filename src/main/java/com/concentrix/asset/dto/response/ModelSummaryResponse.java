package com.concentrix.asset.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModelSummaryResponse {
    Integer modelId;
    String modelName;
    int total;
    List<SiteSummaryResponse> sites;
}