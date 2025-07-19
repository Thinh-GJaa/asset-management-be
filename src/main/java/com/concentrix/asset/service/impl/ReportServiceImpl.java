package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.response.ReportSummaryResponse;
import com.concentrix.asset.dto.response.ReportDetailResponse;
import com.concentrix.asset.dto.response.StatusReportResponse;
import com.concentrix.asset.dto.response.SiteDeviceWithoutSerialSummaryResponse;
import com.concentrix.asset.dto.response.DeviceWithoutSerialSummaryResponse;
import com.concentrix.asset.dto.response.DeviceResponse;
import com.concentrix.asset.dto.response.TypeSummaryResponse;
import com.concentrix.asset.dto.response.ModelSummaryResponse;
import com.concentrix.asset.dto.response.SiteSummaryResponse;
import com.concentrix.asset.entity.*;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.enums.TransactionType;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.ReportService;
import com.concentrix.asset.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ReportServiceImpl implements ReportService {
        SiteRepository siteRepository;
        WarehouseRepository warehouseRepository;
        FloorRepository floorRepository;
        DeviceRepository deviceRepository;
        ModelRepository modelRepository;
        DeviceWarehouseRepository deviceWarehouseRepository;
        TransactionDetailRepository transactionDetailRepository;
        DeviceMapper deviceMapper;

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

        @Override
        public List<StatusReportResponse> getStatusReport(Integer siteId, Integer floorId, Integer warehouseId,
                        TransactionType type, Integer modelId) {
                return List.of();
        }

        @Override
        public Map<String, Map<String, Integer>> getStatusSummaryAllSite() {
                Map<String, Map<String, Integer>> result = new LinkedHashMap<>();

                for (DeviceStatus status : DeviceStatus.values()) {
                        int withSerial = deviceRepository.countByStatusAndSerialNumberIsNotNull(status);
                        int withoutSerial = 0;
                        switch (status) {
                                case IN_STOCK:
                                        withoutSerial = deviceWarehouseRepository.sumAllStock();
                                        break;
                                case IN_FLOOR:
                                        int in = transactionDetailRepository.sumAllUseFloor();
                                        int out = transactionDetailRepository.sumAllReturnFromFloor();
                                        withoutSerial = in - out;
                                        break;
                                case ON_THE_MOVE:
                                        withoutSerial = transactionDetailRepository.sumAllOnTheMove();
                                        break;
                                case DISPOSED:
                                        withoutSerial = transactionDetailRepository.sumAllDisposal();
                                        ;
                                        break;
                                case E_WASTE:
                                        withoutSerial = transactionDetailRepository.sumAllEWaste();
                                        ;
                                        break;
                                case REPAIR:
                                        int repairIn = transactionDetailRepository.sumAllRepair();
                                        int repairOut = transactionDetailRepository.sumAllReturnFromRepair();
                                        withoutSerial = repairIn - repairOut;
                                        break;
                                case ASSIGNED:
                                        int assignment = transactionDetailRepository.sumAllAssignment();
                                        int returnFromAssignment = transactionDetailRepository.sumAllReturnFromUser();
                                        withoutSerial = assignment - returnFromAssignment;
                                        break;
                                default:
                                        withoutSerial = 0;
                        }
                        Map<String, Integer> statusMap = new HashMap<>();
                        statusMap.put("withSerial", withSerial);
                        statusMap.put("withoutSerial", withoutSerial);
                        result.put(status.name(), statusMap);
                }
                return result;
        }

        @Override
        public List<SiteDeviceWithoutSerialSummaryResponse> getWithoutSerialSummary(DeviceStatus status,
                        DeviceType type, Integer modelId) {
                List<Site> sites = siteRepository.findAll();
                List<DeviceType> types = (type != null) ? List.of(type) : Arrays.asList(DeviceType.values());
                List<SiteDeviceWithoutSerialSummaryResponse> result = new ArrayList<>();

                for (Site site : sites) {
                        List<DeviceWithoutSerialSummaryResponse> typeSummaries = new ArrayList<>();
                        for (DeviceType t : types) {
                                List<Model> models = modelRepository.findByType(t);
                                int typeTotal = 0;
                                List<DeviceWithoutSerialSummaryResponse.ModelQuantity> modelQuantities = new ArrayList<>();
                                for (Model model : models) {
                                        if (modelId != null && !model.getModelId().equals(modelId))
                                                continue;
                                        int quantity = 0;
                                        switch (status) {
                                                case IN_STOCK:
                                                        quantity = deviceWarehouseRepository.sumQuantityInStockBySite(
                                                                        t, model.getModelId(), site.getSiteId());
                                                        break;
                                                case IN_FLOOR:
                                                        int in = transactionDetailRepository.sumFloorInBySite(
                                                                        t, model.getModelId(), site.getSiteId());
                                                        int out = transactionDetailRepository.sumFloorOutSite(
                                                                        t, model.getModelId(), site.getSiteId());
                                                        quantity = in - out;
                                                        break;
                                                case ON_THE_MOVE:
                                                        quantity = transactionDetailRepository.sumOnTheMove(
                                                                        t, model.getModelId(), site.getSiteId());
                                                        break;
                                                case DISPOSED:
                                                        quantity = transactionDetailRepository.sumDisposed(
                                                                        t, model.getModelId(), site.getSiteId());
                                                        break;
                                                case E_WASTE:
                                                        quantity = transactionDetailRepository.sumEWaste(
                                                                        t, model.getModelId(), site.getSiteId());
                                                        break;
                                                default:
                                                        quantity = 0;
                                        }
                                        if (quantity > 0) {
                                                DeviceWithoutSerialSummaryResponse.ModelQuantity mq = new DeviceWithoutSerialSummaryResponse.ModelQuantity();
                                                mq.setModelId(model.getModelId());
                                                mq.setModelName(model.getModelName());
                                                mq.setQuantity(quantity);
                                                modelQuantities.add(mq);
                                                typeTotal += quantity;
                                        }
                                }
                                if (typeTotal > 0) {
                                        DeviceWithoutSerialSummaryResponse summary = new DeviceWithoutSerialSummaryResponse();
                                        summary.setType(t);
                                        summary.setTotal(typeTotal);
                                        summary.setModels(modelQuantities);
                                        typeSummaries.add(summary);
                                }
                        }
                        if (!typeSummaries.isEmpty()) {
                                SiteDeviceWithoutSerialSummaryResponse siteSummary = new SiteDeviceWithoutSerialSummaryResponse();
                                siteSummary.setSiteId(site.getSiteId());
                                siteSummary.setSiteName(site.getSiteName());
                                siteSummary.setTypes(typeSummaries);
                                result.add(siteSummary);
                        }
                }
                return result;
        }

        @Override
        public List<TypeSummaryResponse> getWithSerialSummary(Integer siteId, DeviceStatus status,
                        Integer floorId, DeviceType type, Integer modelId) {
                List<DeviceType> types = (type != null) ? List.of(type) : Arrays.asList(DeviceType.values());
                List<Site> sites = (siteId == null) ? siteRepository.findAll()
                        : Collections.singletonList(siteRepository.findById(siteId).orElse(null));
                List<TypeSummaryResponse> typeSummaries = new ArrayList<>();

                for (DeviceType t : types) {
                        List<Model> models = modelRepository.findByType(t);
                        int typeTotal = 0;
                        List<ModelSummaryResponse> modelSummaries = new ArrayList<>();
                        for (Model model : models) {
                                if (modelId != null && !model.getModelId().equals(modelId))
                                        continue;
                                int modelTotal = 0;
                                List<SiteSummaryResponse> siteSummaries = new ArrayList<>();
                                switch (status) {
                                        case IN_STOCK -> {
                                                for (Site site : sites) {
                                                        if (site == null) continue;
                                                        int quantity = deviceRepository.countAssetInStock(site.getSiteId(), t, model.getModelId());
                                                        if (quantity > 0) {
                                                                SiteSummaryResponse siteSummary = new SiteSummaryResponse();
                                                                siteSummary.setSiteId(site.getSiteId());
                                                                siteSummary.setSiteName(site.getSiteName());
                                                                siteSummary.setTotal(quantity);
                                                                siteSummaries.add(siteSummary);
                                                                modelTotal += quantity;
                                                        }
                                                }
                                        }
                                        case IN_FLOOR -> {
                                                for (Site site : sites) {
                                                        if (site == null) continue;
                                                        for (Floor floor : floorRepository.findAllBySite_SiteId(site.getSiteId())) {
                                                                int quantity = deviceRepository.countAssetInFloor(site.getSiteId(), floor.getFloorId(), t, model.getModelId());
                                                                if (quantity > 0) {
                                                                        SiteSummaryResponse siteSummary = new SiteSummaryResponse();
                                                                        siteSummary.setSiteId(site.getSiteId());
                                                                        siteSummary.setSiteName(site.getSiteName());
                                                                        siteSummary.setTotal(quantity);
                                                                        siteSummaries.add(siteSummary);
                                                                        modelTotal += quantity;
                                                                }
                                                        }
                                                }
                                        }
                                        case ON_THE_MOVE -> {
                                                int quantity = deviceRepository.countAssetOnTheMove(t, model.getModelId());
                                                if (quantity > 0) {
                                                        modelTotal += quantity;
                                                }
                                        }
                                        default -> {
                                                int quantity = deviceRepository.countAssetByStatus(status, t, model.getModelId());
                                                if (quantity > 0) {
                                                        modelTotal += quantity;
                                                }
                                        }
                                }
                                if (modelTotal > 0) {
                                        ModelSummaryResponse modelSummary = new ModelSummaryResponse();
                                        modelSummary.setModelId(model.getModelId());
                                        modelSummary.setModelName(model.getModelName());
                                        modelSummary.setTotal(modelTotal);
                                        // Chỉ IN_STOCK và IN_FLOOR mới có siteSummaries
                                        if (status == DeviceStatus.IN_STOCK || status == DeviceStatus.IN_FLOOR) {
                                                modelSummary.setSites(siteSummaries);
                                        }
                                        modelSummaries.add(modelSummary);
                                        typeTotal += modelTotal;
                                }
                        }
                        if (typeTotal > 0) {
                                TypeSummaryResponse typeSummary = new TypeSummaryResponse();
                                typeSummary.setType(t);
                                typeSummary.setTotal(typeTotal);
                                typeSummary.setModels(modelSummaries);
                                typeSummaries.add(typeSummary);
                        }
                }
                return typeSummaries;
        }

        @Override
        public List<DeviceResponse> getDeviceListForReport(Integer siteId, DeviceStatus status, Integer floorId,
                        DeviceType type, Integer modelId) {
                log.info("[ReportServiceImpl][getDeviceListForReport]: siteId={}, status={}, floorId={}, type={}, modelId={}",
                                siteId, status, floorId, type, modelId);
                List<Device> devices = new ArrayList<>();
                switch (status) {
                        case IN_STOCK -> {
                                devices = deviceRepository.findDevicesInStockForReport(siteId, type, modelId);
                                break;
                        }
                        case IN_FLOOR -> {
                                devices = deviceRepository.findDevicesInFloorForReport(siteId, floorId, type, modelId);
                                break;
                        }
                        case ON_THE_MOVE -> {
                                devices = deviceRepository.findDevicesOnTheMoveForReport(type, modelId);
                                break;
                        }
                        case DISPOSED, E_WASTE, REPAIR, ASSIGNED -> {
                                devices = deviceRepository.findDevicesStatusForReport(status, type, modelId);
                                break;
                        }
                        default -> {
                                devices = null;
                        }

                }

                return devices.stream().map(deviceMapper::toDeviceResponse).collect(Collectors.toList());
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
