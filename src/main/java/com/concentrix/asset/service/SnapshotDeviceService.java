package com.concentrix.asset.service;

import com.concentrix.asset.dto.response.DataTrending;
import com.concentrix.asset.dto.response.DeviceChangeDetail;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;

public interface SnapshotDeviceService {

    void snapshotDataDevice();

    DataTrending getDataTrending(Integer siteId, DeviceStatus status, DeviceType type);

    DeviceChangeDetail getDeviceChanges(String fromDate, String toDate, Integer siteId, DeviceStatus status,
            DeviceType type);

}