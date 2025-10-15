package com.concentrix.asset.service;

import com.concentrix.asset.dto.response.CompareDataResponse;
import com.concentrix.asset.dto.response.DeviceChangesResponse;
import com.concentrix.asset.dto.response.DeviceChangeDetail;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;

import java.time.LocalDate;

public interface SnapshotDeviceService {

    void snapshotDataDevice();

    CompareDataResponse getDataCompare(LocalDate fromDate, LocalDate toDate, String groupBy, Integer siteId,
                                       DeviceStatus status, DeviceType type);

    DeviceChangesResponse getDeviceChanges(LocalDate fromDate, LocalDate toDate, Integer siteId,
                                          DeviceStatus status, DeviceType type);

}