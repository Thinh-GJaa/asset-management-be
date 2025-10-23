package com.concentrix.asset.service;

import com.concentrix.asset.dto.response.DeviceResponse;
import com.concentrix.asset.dto.response.SiteDeviceWithoutSerialSummaryResponse;
import com.concentrix.asset.dto.response.SiteTypeChartResponse;
import com.concentrix.asset.dto.response.TypeSummaryResponse;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;

import java.util.List;
import java.util.Map;

public interface ReportService {

        Map<String, Map<String, Integer>> getStatusSummaryAllSite();

        List<SiteDeviceWithoutSerialSummaryResponse> getWithoutSerialSummary(DeviceStatus status, DeviceType type,
                        Integer modelId);

        List<TypeSummaryResponse> getWithSerialSummary(
                        Integer siteId, DeviceStatus status, Integer floorId, Integer ownerId, Integer accountId,
                        DeviceType type, Integer modelId, Boolean isOutOfWarranty, String ageRange);

        List<DeviceResponse> getDeviceListForReport(
                        Integer siteId, DeviceStatus status, Integer floorId, Integer ownerId, Integer accountId,
                        DeviceType type, Integer modelId, Boolean isOutOfWarranty, String ageRange);

        List<SiteTypeChartResponse> getSiteTypeChartWithSerial(DeviceStatus status);

        List<SiteTypeChartResponse> getSiteTypeChartWithoutSerial(DeviceStatus status);

        byte[] generateDeviceListCsv(
                        Integer siteId, DeviceStatus status, Integer floorId, Integer ownerId, Integer accountId,
                        DeviceType type, Integer modelId, Boolean isOutOfWarranty, String ageRange);
}