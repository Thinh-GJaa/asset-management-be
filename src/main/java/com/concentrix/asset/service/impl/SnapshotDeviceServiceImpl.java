package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.response.DataTrending;
import com.concentrix.asset.dto.response.DeviceChangeDetail;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.Predicate;

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
                    .snapshotDate(LocalDate.now())
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
    public DataTrending getDataTrending(Integer siteId, DeviceStatus status, DeviceType type) {
        // Lấy 7 mốc thời gian snapshot gần nhất (đã được sắp xếp giảm dần từ DB)
        List<LocalDate> last7Dates = snapshotDeviceRepository
                .findDistinctSnapshotDatesOrderByDesc()
                .stream()
                .limit(7)
                .sorted() // Đảo ngược để có thứ tự tăng dần cho chart
                .toList();

        if (last7Dates.isEmpty()) {
            return DataTrending.builder()
                    .labels(new ArrayList<>())
                    .datasets(new ArrayList<>())
                    .build();
        }

        // Format labels cho chart (dd/MM/yyyy)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<String> labels = last7Dates.stream()
                .map(date -> date.format(formatter))
                .collect(Collectors.toList());

        // Đếm số lượng device cho mỗi mốc thời gian với filter
        List<Integer> datasets = new ArrayList<>();
        for (LocalDate date : last7Dates) {
            Specification<SnapshotDevice> spec = buildSpecification(date, siteId, status, type);
            long count = snapshotDeviceRepository.count(spec);
            datasets.add((int) count);
        }

        return DataTrending.builder()
                .labels(labels)
                .datasets(datasets)
                .build();
    }

    /**
     * Xây dựng Specification để filter dữ liệu snapshot theo các điều kiện
     *
     * @param snapshotDate Ngày snapshot cần lọc
     * @param siteId       ID của site (có thể null)
     * @param status       Trạng thái device (có thể null)
     * @param type         Loại device (có thể null)
     * @return Specification để filter
     */
    private Specification<SnapshotDevice> buildSpecification(LocalDate snapshotDate,
            Integer siteId,
            DeviceStatus status,
            DeviceType type) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter theo snapshot date (bắt buộc)
            predicates.add(criteriaBuilder.equal(root.get("snapshotDate"), snapshotDate));

            // Filter theo site (nếu có)
            if (siteId != null) {
                predicates.add(criteriaBuilder.equal(root.get("site").get("siteId"), siteId));
            }

            // Filter theo status (nếu có)
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // Filter theo device type (nếu có)
            if (type != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("device").get("model").get("type"), type));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public DeviceChangeDetail getDeviceChanges(String fromDate, String toDate,
            Integer siteId, DeviceStatus status, DeviceType type) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate from = LocalDate.parse(fromDate, formatter);
        LocalDate to = LocalDate.parse(toDate, formatter);

        // Lấy danh sách snapshot tại 2 mốc thời gian
        Specification<SnapshotDevice> specFrom = buildSpecification(from, siteId, status, type);
        Specification<SnapshotDevice> specTo = buildSpecification(to, siteId, status, type);

        List<SnapshotDevice> snapshotsFrom = snapshotDeviceRepository.findAll(specFrom);
        List<SnapshotDevice> snapshotsTo = snapshotDeviceRepository.findAll(specTo);

        // Tạo Set deviceId từ 2 danh sách để so sánh
        Set<Integer> deviceIdsFrom = snapshotsFrom.stream()
                .map(s -> s.getDevice().getDeviceId())
                .collect(Collectors.toSet());

        Set<Integer> deviceIdsTo = snapshotsTo.stream()
                .map(s -> s.getDevice().getDeviceId())
                .collect(Collectors.toSet());

        // Tìm devices được thêm (có trong TO nhưng không có trong FROM)
        List<SnapshotDevice> addedSnapshots = snapshotsTo.stream()
                .filter(s -> !deviceIdsFrom.contains(s.getDevice().getDeviceId()))
                .collect(Collectors.toList());

        // Tìm devices bị xóa (có trong FROM nhưng không có trong TO)
        List<SnapshotDevice> removedSnapshots = snapshotsFrom.stream()
                .filter(s -> !deviceIdsTo.contains(s.getDevice().getDeviceId()))
                .collect(Collectors.toList());

        // Convert sang DeviceInfo
        List<DeviceChangeDetail.DeviceInfo> addedDevices = addedSnapshots.stream()
                .map(this::convertToDeviceInfo)
                .collect(Collectors.toList());

        List<DeviceChangeDetail.DeviceInfo> removedDevices = removedSnapshots.stream()
                .map(this::convertToDeviceInfo)
                .collect(Collectors.toList());

        return DeviceChangeDetail.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .totalAdded(addedDevices.size())
                .totalRemoved(removedDevices.size())
                .netChange(addedDevices.size() - removedDevices.size())
                .addedDevices(addedDevices)
                .removedDevices(removedDevices)
                .build();
    }

    /**
     * Convert SnapshotDevice sang DeviceInfo DTO
     */
    private DeviceChangeDetail.DeviceInfo convertToDeviceInfo(SnapshotDevice snapshot) {
        Device device = snapshot.getDevice();
        return DeviceChangeDetail.DeviceInfo.builder()
                .deviceId(device.getDeviceId())
                .serialNumber(device.getSerialNumber())
                .deviceName(device.getDeviceName())
                .modelName(device.getModel() != null ? device.getModel().getModelName() : null)
                .type(device.getModel() != null ? device.getModel().getType().name() : null)
                .status(snapshot.getStatus() != null ? snapshot.getStatus().name() : null)
                .siteName(snapshot.getSite() != null ? snapshot.getSite().getSiteName() : null)
                .build();
    }

}