package com.concentrix.asset.dto.response;

import lombok.Data;

@Data
public class SiteSummaryResponse {
    private Integer siteId;
    private String siteName;
    private int total;
}