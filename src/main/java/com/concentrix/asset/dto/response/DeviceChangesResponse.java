package com.concentrix.asset.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class DeviceChangesResponse implements Serializable {

    LocalDate fromDate;
    LocalDate toDate;

    // Thống kê tổng quan
    Integer totalAdded;
    Integer totalRemoved;
    Integer netChange;

    // Danh sách thiết bị được thêm
    List<DeviceChangeItem> addedDevices;

    // Danh sách thiết bị bị xóa
    List<DeviceChangeItem> removedDevices;

}
