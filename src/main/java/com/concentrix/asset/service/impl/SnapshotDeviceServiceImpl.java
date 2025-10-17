package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.response.CompareDataResponse;
import com.concentrix.asset.dto.response.DeviceChangeItem;
import com.concentrix.asset.dto.response.DeviceChangesResponse;
import com.concentrix.asset.entity.Device;
import com.concentrix.asset.entity.SnapshotDevice;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.repository.DeviceRepository;
import com.concentrix.asset.repository.SnapshotDeviceRepository;
import com.concentrix.asset.service.SnapshotDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class SnapshotDeviceServiceImpl implements SnapshotDeviceService {

    DeviceRepository deviceRepository;
    SnapshotDeviceRepository snapshotDeviceRepository;

    @Override
    public void snapshotDataDevice() {

        List<SnapshotDevice> snapshotList = new ArrayList<>();

        List<Device> devices = deviceRepository.findAllBySerialNumberIsNotNull();
        for (Device device : devices) {
            SnapshotDevice snapshotDevice = SnapshotDevice.builder()
                    .snapshotDate(LocalDate.now().plusDays(1))
                    .device(device)
                    .status(device.getStatus())
                    .site(
                            device.getCurrentWarehouse() != null
                                    ? device.getCurrentWarehouse().getSite()
                                    : (device.getCurrentFloor() != null ? device.getCurrentFloor().getSite() : null))
                    .build();
            snapshotList.add(snapshotDevice);
        }
        snapshotDeviceRepository.saveAll(snapshotList);

    }

    @Override
    public CompareDataResponse getDataCompare(LocalDate fromDate, LocalDate toDate, String groupBy, Integer siteId,
                    DeviceStatus status, DeviceType type) {

        try {
            // Query dữ liệu cho thời điểm fromDate
            List<SnapshotDevice> fromSnapshotDevices = snapshotDeviceRepository.getSnapshotDevicesByDate(fromDate, siteId, status,
                    type);

            // Query dữ liệu cho thời điểm toDate
            List<SnapshotDevice> toSnapshotDevices = snapshotDeviceRepository.getSnapshotDevicesByDate(toDate, siteId, status, type);

            // Tính toán sự khác biệt
            return calculateComparisonData(fromSnapshotDevices, toSnapshotDevices, fromDate, toDate,
                    groupBy);

        } catch (Exception e) {
            log.error("Error comparing data between dates: {}", e.getMessage());
            return CompareDataResponse.builder()
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .build();
        }
    }


    @Override
    public DeviceChangesResponse getDeviceChanges(LocalDate fromDate, LocalDate toDate, Integer siteId,
                                                 DeviceStatus status, DeviceType type) {

        try {
            // Query dữ liệu cho thời điểm fromDate
            List<SnapshotDevice> fromSnapshotDevices = getSnapshotDevicesByDate(fromDate, siteId, status, type);

            // Query dữ liệu cho thời điểm toDate
            List<SnapshotDevice> toSnapshotDevices = getSnapshotDevicesByDate(toDate, siteId, status, type);

            // Tính toán thiết bị được thêm/bớt
            List<DeviceChangeItem> addedDevices = findAddedDevices(fromSnapshotDevices, toSnapshotDevices);
            log.info("[Check] addedDevices: {}", addedDevices.size());

            List<DeviceChangeItem> removedDevices = findRemovedDevices(fromSnapshotDevices, toSnapshotDevices);
            log.info("[Check] removedDevices: {}", removedDevices.size());

            // Tính tổng số
            int totalAdded = addedDevices.size();
            int totalRemoved = removedDevices.size();
            int netChange = totalAdded - totalRemoved;

            return DeviceChangesResponse.builder()
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .totalAdded(totalAdded)
                    .totalRemoved(totalRemoved)
                    .netChange(netChange)
                    .addedDevices(addedDevices)
                    .removedDevices(removedDevices)
                    .build();

        } catch (Exception e) {
            log.error("Error getting device changes between dates: {}", e.getMessage());
            return DeviceChangesResponse.builder()
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .build();
        }
    }

    private List<SnapshotDevice> getSnapshotDevicesByDate(LocalDate date, Integer siteId, DeviceStatus status,
                    DeviceType type) {
        // Lấy tất cả snapshot devices và filter theo ngày
        return snapshotDeviceRepository.findAll()
                        .stream()
                        .filter(snapshot -> snapshot.getSnapshotDate().equals(date))
                        .filter(snapshot -> {
                            // Filter theo site nếu có
                            if (siteId != null && snapshot.getSite() != null) {
                                return snapshot.getSite().getSiteId().equals(siteId);
                            }
                            // Filter theo status nếu có
                            if (status != null) {
                                return snapshot.getStatus().equals(status);
                            }
                            // Filter theo type nếu có (lấy từ model của device)
                            if (type != null && snapshot.getDevice().getModel() != null) {
                                return snapshot.getDevice().getModel().getType().equals(type);
                            }
                            return true; // Không filter
                        })
                        .collect(Collectors.toList());
    }

    private CompareDataResponse calculateComparisonData(List<SnapshotDevice> fromDevices,
                    List<SnapshotDevice> toDevices,
                    LocalDate fromDate,
                    LocalDate toDate,
                    String groupBy) {

        // Tính toán dữ liệu trending theo nhóm
        Map<String, List<Integer>> datasets = calculateGroupedDatasets(fromDevices, toDevices, groupBy);

        return CompareDataResponse.builder()
                        .fromDate(fromDate)
                        .toDate(toDate)
                        .datasets(datasets)
                        .build();
    }

    private Map<String, List<Integer>> calculateGroupedDatasets(List<SnapshotDevice> fromDevices,
                    List<SnapshotDevice> toDevices,
                    String groupBy) {
        Map<String, List<Integer>> datasets = new HashMap<>();

        // Nhóm dữ liệu theo groupBy field
        Map<String, Long> fromCounts = groupAndCount(fromDevices, groupBy);
        Map<String, Long> toCounts = groupAndCount(toDevices, groupBy);

        // Lấy tất cả các nhóm từ cả 2 thời điểm
        Set<String> allGroups = new HashSet<>();
        allGroups.addAll(fromCounts.keySet());
        allGroups.addAll(toCounts.keySet());

        // Với mỗi nhóm, tạo [count_from, count_to]
        for (String group : allGroups) {
            long fromCount = fromCounts.getOrDefault(group, 0L);
            long toCount = toCounts.getOrDefault(group, 0L);
            datasets.put(group, Arrays.asList((int) fromCount, (int) toCount));
        }

        return datasets;
    }

    private Map<String, Long> groupAndCount(List<SnapshotDevice> devices, String groupBy) {
        return devices.stream()
                        .collect(Collectors.groupingBy(
                                        snapshot -> getGroupKey(snapshot, groupBy),
                                        Collectors.counting()));
    }

    private String getGroupKey(SnapshotDevice snapshot, String groupBy) {
        return switch (groupBy != null ? groupBy.toLowerCase() : "") {
            case "site" -> snapshot.getSite() != null ? snapshot.getSite().getSiteName() : "No Site";
            case "status" -> snapshot.getStatus() != null ? snapshot.getStatus().toString() : "No Status";
            case "type" -> snapshot.getDevice().getModel() != null
                    ? snapshot.getDevice().getModel().getType().toString()
                    : "No Type";
            default -> "All"; // Không group
        };
    }

    private List<DeviceChangeItem> findAddedDevices(List<SnapshotDevice> fromDevices,
                                                   List<SnapshotDevice> toDevices) {
        Set<String> fromSerials = fromDevices.stream()
                .map(snapshot -> snapshot.getDevice().getSerialNumber())
                .collect(Collectors.toSet());

        return toDevices.stream()
                .filter(toDevice -> !fromSerials.contains(toDevice.getDevice().getSerialNumber()))
                .map(this::convertToDeviceChangeItem)
                .collect(Collectors.toList());
    }

    private List<DeviceChangeItem> findRemovedDevices(List<SnapshotDevice> fromDevices,
                                                     List<SnapshotDevice> toDevices) {
        Set<String> toSerials = toDevices.stream()
                .map(snapshot -> snapshot.getDevice().getSerialNumber())
                .collect(Collectors.toSet());

        return fromDevices.stream()
                .filter(fromDevice -> !toSerials.contains(fromDevice.getDevice().getSerialNumber()))
                .map(this::convertToDeviceChangeItem)
                .collect(Collectors.toList());
    }

    private DeviceChangeItem convertToDeviceChangeItem(SnapshotDevice snapshot) {
        Device device = snapshot.getDevice();
        String siteName = snapshot.getSite() != null ? snapshot.getSite().getSiteName() : "N/A";

        return DeviceChangeItem.builder()
                .serialNumber(device.getSerialNumber())
                .deviceName(device.getDeviceName())
                .modelName(device.getModel() != null ? device.getModel().getModelName() : "N/A")
                .type(device.getModel() != null ? device.getModel().getType().toString() : "N/A")
                .status(snapshot.getStatus() != null ? snapshot.getStatus().toString() : "N/A")
                .siteName(siteName)
                .build();
    }
}
