package com.concentrix.asset.service;

import com.concentrix.asset.dto.response.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.enums.TransactionType;

import java.util.List;
import java.util.Map;

public interface ReportService {

    List<ReportSummaryResponse> getReportSummary(Integer siteId);

    ReportDetailResponse getReportDetail(Integer siteId, Integer floorId, Integer warehouseId, String type,
                                         Integer modelId);

    List<StatusReportResponse> getStatusReport(Integer siteId, Integer floorId, Integer warehouseId,
                                               TransactionType type, Integer modelId);

    /**
     * Tổng hợp số lượng thiết bị theo tất cả trạng thái, phân biệt
     * withSerial/withoutSerial, không phụ thuộc site
     *
     * @return Map<String, Map < String, Long>>
     */
    Map<String, Map<String, Integer>> getStatusSummaryAllSite();

    List<SiteDeviceWithoutSerialSummaryResponse> getWithoutSerialSummary(DeviceStatus status, DeviceType type,
                                                                         Integer modelId);

    List<TypeSummaryResponse> getWithSerialSummary(Integer siteId, DeviceStatus status, Integer floorId, DeviceType type,
                                                   Integer modelId);

    List<DeviceResponse> getDeviceListForReport(Integer siteId, DeviceStatus status, Integer floorId, DeviceType type, Integer modelId);

    StatusSummaryResponse getStatusSummaryWithSerial(DeviceStatus status);

    StatusSummaryResponse getStatusSummaryWithoutSerial(DeviceStatus status);
}