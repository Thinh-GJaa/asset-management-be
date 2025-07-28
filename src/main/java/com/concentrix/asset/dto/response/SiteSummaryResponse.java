package com.concentrix.asset.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SiteSummaryResponse {
    Integer siteId;
    String siteName;
    int total;
}