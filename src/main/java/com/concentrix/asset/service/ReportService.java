package com.concentrix.asset.service;

import com.concentrix.asset.dto.response.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.enums.TransactionType;

import java.util.List;
import java.util.Map;

public interface ReportService {

    Map<String, Map<String, Integer>> getStatusSummaryAllSite();

    List<SiteDeviceWithoutSerialSummaryResponse> getWithoutSerialSummary(DeviceStatus status, DeviceType type,
            Integer modelId);

    List<TypeSummaryResponse> getWithSerialSummary(Integer siteId, DeviceStatus status, Integer floorId, Integer accountId,
            DeviceType type, Integer modelId);

    List<DeviceResponse> getDeviceListForReport(Integer siteId, DeviceStatus status, Integer floorId, Integer accountId, DeviceType type,
            Integer modelId);

    StatusSummaryResponse getStatusSummaryWithSerial(DeviceStatus status);

    StatusSummaryResponse getStatusSummaryWithoutSerial(DeviceStatus status);

    List<SiteTypeChartResponse> getSiteTypeChartWithSerial(DeviceStatus status);

    List<SiteTypeChartResponse> getSiteTypeChartWithoutSerial(DeviceStatus status);
}