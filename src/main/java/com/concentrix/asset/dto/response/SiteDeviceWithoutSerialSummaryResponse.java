package com.concentrix.asset.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class SiteDeviceWithoutSerialSummaryResponse {
    private Integer siteId;
    private String siteName;
    private List<DeviceWithoutSerialSummaryResponse> types;
}