package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.response.ReportSummaryResponse;
import com.concentrix.asset.dto.response.ReportDetailResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ReportServiceImpl implements ReportService {
        SiteRepository siteRepository;
        WarehouseRepository warehouseRepository;
        FloorRepository floorRepository;
        DeviceRepository deviceRepository;
        ModelRepository modelRepository;

        @Override
        public List<ReportSummaryResponse> getReportSummary(Integer siteId) {
                List<Site> sites = (siteId == null) ? siteRepository.findAll()
                                : Collections.singletonList(siteRepository.findById(siteId).orElse(null));
                List<ReportSummaryResponse> result = new ArrayList<>();
                for (Site site : sites) {
                        if (site == null)
                                continue;
                        List<Warehouse> warehouses = warehouseRepository.findAllBySite_SiteId(site.getSiteId());
                        List<Floor> floors = floorRepository.findAllBySite_SiteId(site.getSiteId());
                        Set<Integer> warehouseIds = warehouses.stream().map(Warehouse::getWarehouseId)
                                        .collect(Collectors.toSet());
                        Set<Integer> floorIds = floors.stream().map(Floor::getFloorId).collect(Collectors.toSet());
                        List<Device> devices = deviceRepository.findAll().stream()
                                        .filter(d -> (d.getCurrentWarehouse() != null && warehouseIds
                                                        .contains(d.getCurrentWarehouse().getWarehouseId()))
                                                        || (d.getCurrentFloor() != null && floorIds
                                                                        .contains(d.getCurrentFloor().getFloorId())))
                                        .collect(Collectors.toList());
                        // Tổng hợp theo trạng thái
                        ReportSummaryResponse.DeviceStatusCount statusCount = countByStatus(devices);
                        // Tổng hợp theo warehouse
                        List<ReportSummaryResponse.WarehouseSummary> warehouseSummaries = warehouses.stream()
                                        .map(wh -> ReportSummaryResponse.WarehouseSummary.builder()
                                                        .warehouseId(wh.getWarehouseId())
                                                        .warehouseName(wh.getWarehouseName())
                                                        .statusCount(countByStatus(devices.stream()
                                                                        .filter(d -> wh.equals(d.getCurrentWarehouse()))
                                                                        .collect(Collectors.toList())))
                                                        .build())
                                        .collect(Collectors.toList());
                        // Tổng hợp theo floor
                        List<ReportSummaryResponse.FloorSummary> floorSummaries = floors.stream()
                                        .map(f -> ReportSummaryResponse.FloorSummary.builder()
                                                        .floorId(f.getFloorId())
                                                        .floorName(f.getFloorName())
                                                        .statusCount(countByStatus(devices.stream()
                                                                        .filter(d -> f.equals(d.getCurrentFloor()))
                                                                        .collect(Collectors.toList())))
                                                        .build())
                                        .collect(Collectors.toList());
                        // Tổng hợp theo type
                        List<ReportSummaryResponse.TypeSummary> typeSummaries = devices.stream()
                                        .collect(Collectors.groupingBy(d -> d.getModel().getType())).entrySet().stream()
                                        .map(e -> ReportSummaryResponse.TypeSummary.builder()
                                                        .type(e.getKey().name())
                                                        .statusCount(countByStatus(e.getValue()))
                                                        .build())
                                        .collect(Collectors.toList());
                        // Tổng hợp theo model
                        List<ReportSummaryResponse.ModelSummary> modelSummaries = devices.stream()
                                        .collect(Collectors.groupingBy(d -> d.getModel())).entrySet().stream()
                                        .map(e -> ReportSummaryResponse.ModelSummary.builder()
                                                        .modelId(e.getKey().getModelId())
                                                        .modelName(e.getKey().getModelName())
                                                        .statusCount(countByStatus(e.getValue()))
                                                        .build())
                                        .collect(Collectors.toList());
                        result.add(ReportSummaryResponse.builder()
                                        .siteId(site.getSiteId())
                                        .siteName(site.getSiteName())
                                        .warehouses(warehouseSummaries)
                                        .floors(floorSummaries)
                                        .types(typeSummaries)
                                        .models(modelSummaries)
                                        .statusCount(statusCount)
                                        .build());
                }
                return result;
        }

        @Override
        public ReportDetailResponse getReportDetail(Integer siteId, Integer floorId, Integer warehouseId, String type,
                        Integer modelId) {
                // Lấy tất cả site hoặc theo filter
                List<Site> sites = (siteId == null) ? siteRepository.findAll()
                                : Collections.singletonList(siteRepository.findById(siteId).orElse(null));
                List<ReportDetailResponse.SiteNode> siteNodes = new ArrayList<>();
                int totalAll = 0;
                for (Site site : sites) {
                        if (site == null)
                                continue;
                        // Floor
                        List<Floor> floors = floorRepository.findAllBySite_SiteId(site.getSiteId());
                        if (floorId != null)
                                floors = floors.stream().filter(f -> f.getFloorId().equals(floorId)).toList();
                        List<ReportDetailResponse.FloorNode> floorNodes = new ArrayList<>();
                        // Warehouse
                        List<Warehouse> warehouses = warehouseRepository.findAllBySite_SiteId(site.getSiteId());
                        if (warehouseId != null)
                                warehouses = warehouses.stream().filter(w -> w.getWarehouseId().equals(warehouseId))
                                                .toList();
                        List<ReportDetailResponse.WarehouseNode> warehouseNodes = new ArrayList<>();
                        int totalSite = 0;
                        // Floor drill-down
                        for (Floor floor : floors) {
                                List<Device> devices = deviceRepository.findAll().stream()
                                                .filter(d -> d.getCurrentFloor() != null && d.getCurrentFloor()
                                                                .getFloorId().equals(floor.getFloorId()))
                                                .toList();
                                List<ReportDetailResponse.TypeNode> typeNodes = buildTypeNodes(devices, type, modelId);
                                int totalFloor = typeNodes.stream().mapToInt(ReportDetailResponse.TypeNode::getTotal)
                                                .sum();
                                totalSite += totalFloor;
                                floorNodes.add(ReportDetailResponse.FloorNode.builder()
                                                .floorId(floor.getFloorId())
                                                .floorName(floor.getFloorName())
                                                .total(totalFloor)
                                                .types(typeNodes)
                                                .build());
                        }
                        // Warehouse drill-down
                        for (Warehouse warehouse : warehouses) {
                                List<Device> devices = deviceRepository.findAll().stream()
                                                .filter(d -> d.getCurrentWarehouse() != null && d.getCurrentWarehouse()
                                                                .getWarehouseId().equals(warehouse.getWarehouseId()))
                                                .toList();
                                List<ReportDetailResponse.TypeNode> typeNodes = buildTypeNodes(devices, type, modelId);
                                int totalWarehouse = typeNodes.stream()
                                                .mapToInt(ReportDetailResponse.TypeNode::getTotal).sum();
                                totalSite += totalWarehouse;
                                warehouseNodes.add(ReportDetailResponse.WarehouseNode.builder()
                                                .warehouseId(warehouse.getWarehouseId())
                                                .warehouseName(warehouse.getWarehouseName())
                                                .total(totalWarehouse)
                                                .types(typeNodes)
                                                .build());
                        }
                        siteNodes.add(ReportDetailResponse.SiteNode.builder()
                                        .siteId(site.getSiteId())
                                        .siteName(site.getSiteName())
                                        .total(totalSite)
                                        .floors(floorNodes)
                                        .warehouses(warehouseNodes)
                                        .build());
                        totalAll += totalSite;
                }
                return ReportDetailResponse.builder()
                                .total(totalAll)
                                .sites(siteNodes)
                                .build();
        }

        private List<ReportDetailResponse.TypeNode> buildTypeNodes(List<Device> devices, String filterType,
                        Integer filterModelId) {
                Map<String, List<Device>> typeMap = devices.stream()
                                .filter(d -> filterType == null
                                                || d.getModel().getType().name().equalsIgnoreCase(filterType))
                                .collect(Collectors.groupingBy(d -> d.getModel().getType().name()));
                List<ReportDetailResponse.TypeNode> typeNodes = new ArrayList<>();
                for (var entry : typeMap.entrySet()) {
                        String type = entry.getKey();
                        List<Device> typeDevices = entry.getValue();
                        Map<Model, List<Device>> modelMap = typeDevices.stream()
                                        .filter(d -> filterModelId == null
                                                        || d.getModel().getModelId().equals(filterModelId))
                                        .collect(Collectors.groupingBy(Device::getModel));
                        List<ReportDetailResponse.ModelNode> modelNodes = new ArrayList<>();
                        int totalType = 0;
                        for (var modelEntry : modelMap.entrySet()) {
                                Model model = modelEntry.getKey();
                                List<Device> modelDevices = modelEntry.getValue();
                                boolean isSerial = modelDevices.stream().anyMatch(
                                                d -> d.getSerialNumber() != null && !d.getSerialNumber().isEmpty());
                                int totalModel = modelDevices.size();
                                totalType += totalModel;
                                List<String> serials = null;
                                if (isSerial) {
                                        serials = modelDevices.stream()
                                                        .map(Device::getSerialNumber)
                                                        .filter(s -> s != null && !s.isEmpty())
                                                        .toList();
                                }
                                modelNodes.add(ReportDetailResponse.ModelNode.builder()
                                                .modelId(model.getModelId())
                                                .modelName(model.getModelName())
                                                .isSerial(isSerial)
                                                .total(totalModel)
                                                .serials(serials)
                                                .build());
                        }
                        typeNodes.add(ReportDetailResponse.TypeNode.builder()
                                        .type(type)
                                        .total(totalType)
                                        .models(modelNodes)
                                        .build());
                }
                return typeNodes;
        }

        private ReportSummaryResponse.DeviceStatusCount countByStatus(List<Device> devices) {
                return ReportSummaryResponse.DeviceStatusCount.builder()
                                .inUse((int) devices.stream().filter(d -> d.getStatus() == DeviceStatus.IN_FLOOR)
                                                .count())
                                .inStock((int) devices.stream().filter(d -> d.getStatus() == DeviceStatus.IN_STOCK)
                                                .count())
                                .assigned((int) devices.stream().filter(d -> d.getStatus() == DeviceStatus.ASSIGNED)
                                                .count())
                                .disposed((int) devices.stream().filter(d -> d.getStatus() == DeviceStatus.DISPOSED)
                                                .count())
                                .ewaste((int) devices.stream().filter(d -> d.getStatus() == DeviceStatus.E_WASTE)
                                                .count())
                                .build();
        }
}
