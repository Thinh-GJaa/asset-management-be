package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.response.ReportSummaryResponse;
import com.concentrix.asset.dto.response.ReportDetailResponse;
import com.concentrix.asset.dto.response.StatusReportResponse;
import com.concentrix.asset.dto.response.StatusSummaryResponse;
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

                        int withoutSerial = switch (status) {
                                case IN_STOCK -> deviceWarehouseRepository.sumAllStock();

                                case IN_FLOOR -> transactionDetailRepository.sumAllUseFloor()
                                                - transactionDetailRepository.sumAllReturnFromFloor();
                                case ON_THE_MOVE -> transactionDetailRepository.sumAllOnTheMove();

                                case DISPOSED -> transactionDetailRepository.sumAllDisposal();

                                case E_WASTE -> transactionDetailRepository.sumAllEWaste();

                                case REPAIR -> transactionDetailRepository.sumAllRepair()
                                                - transactionDetailRepository.sumAllReturnFromRepair();
                                case ASSIGNED -> transactionDetailRepository.sumAllAssignment()
                                                - transactionDetailRepository.sumAllReturnFromUser();
                                default -> 0;
                        };
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
                                                        int out = transactionDetailRepository.sumFloorOutBySite(
                                                                        t, model.getModelId(), site.getSiteId());
                                                        quantity = in - out;
                                                        break;

                                                case ASSIGNED:
                                                        int assignment = transactionDetailRepository.sumAssignment(
                                                                        t, model.getModelId());
                                                        int returnFromUser = transactionDetailRepository
                                                                        .sumReturnFromUser(t,
                                                                                        model.getModelId());
                                                        quantity = assignment - returnFromUser;
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
                                                        if (site == null)
                                                                continue;
                                                        int quantity = deviceRepository.countAssetInStock(
                                                                        site.getSiteId(), t, model.getModelId());
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
                                                        if (site == null)
                                                                continue;
                                                        for (Floor floor : floorRepository
                                                                        .findAllBySite_SiteId(site.getSiteId())) {
                                                                int quantity = deviceRepository.countAssetInFloor(
                                                                                site.getSiteId(), floor.getFloorId(), t,
                                                                                model.getModelId());
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
                                                int quantity = deviceRepository.countAssetOnTheMove(t,
                                                                model.getModelId());
                                                if (quantity > 0) {
                                                        modelTotal += quantity;
                                                }
                                        }
                                        default -> {
                                                int quantity = deviceRepository.countAssetByStatus(status, t,
                                                                model.getModelId());
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

                List<Device> devices = new ArrayList<>();
                switch (status) {
                        case IN_STOCK -> {
                                devices = deviceRepository.findDevicesInStockForReport(siteId, type, modelId);
                        }
                        case IN_FLOOR -> {
                                devices = deviceRepository.findDevicesInFloorForReport(siteId, floorId, type, modelId);
                        }
                        case ON_THE_MOVE -> {
                                devices = deviceRepository.findDevicesOnTheMoveForReport(type, modelId);
                        }
                        case DISPOSED, E_WASTE, REPAIR, ASSIGNED -> {
                                devices = deviceRepository.findDevicesStatusForReport(status, type, modelId);
                        }
                        default -> {
                                devices = null;
                        }

                }

                return devices.stream().map(deviceMapper::toDeviceResponse).toList();
        }

        @Override
        public StatusSummaryResponse getStatusSummaryWithSerial(DeviceStatus status) {
                List<Device> devices = deviceRepository.findAll().stream()
                                .filter(d -> d.getStatus() == status && d.getSerialNumber() != null)
                                .toList();

                // By Type
                List<TypeSummaryResponse> typeList = devices.stream()
                                .collect(Collectors.groupingBy(d -> d.getModel().getType()))
                                .entrySet().stream()
                                .map(e -> {
                                        TypeSummaryResponse t = new TypeSummaryResponse();
                                        t.setType(e.getKey());
                                        t.setTotal(e.getValue().size());
                                        t.setModels(null);
                                        return t;
                                }).collect(Collectors.toList());

                // By Model
                List<ModelSummaryResponse> modelList = devices.stream()
                                .collect(Collectors.groupingBy(Device::getModel))
                                .entrySet().stream()
                                .map(e -> {
                                        ModelSummaryResponse m = new ModelSummaryResponse();
                                        m.setModelId(e.getKey().getModelId());
                                        m.setModelName(e.getKey().getModelName());
                                        m.setTotal(e.getValue().size());
                                        m.setSites(null);
                                        return m;
                                }).collect(Collectors.toList());

                // By Site
                List<SiteSummaryResponse> siteList = new ArrayList<>();
                switch (status) {
                        case IN_STOCK, E_WASTE:
                                siteList = devices.stream()
                                                .filter(d -> d.getCurrentWarehouse() != null
                                                                && d.getCurrentWarehouse().getSite() != null)
                                                .collect(Collectors.groupingBy(d -> d.getCurrentWarehouse().getSite()))
                                                .entrySet().stream()
                                                .map(e -> {
                                                        SiteSummaryResponse s = new SiteSummaryResponse();
                                                        s.setSiteId(e.getKey().getSiteId());
                                                        s.setSiteName(e.getKey().getSiteName());
                                                        s.setTotal(e.getValue().size());
                                                        return s;
                                                }).toList();
                                break;
                        case IN_FLOOR:
                                siteList = devices.stream()
                                                .filter(d -> d.getCurrentFloor() != null
                                                                && d.getCurrentFloor().getSite() != null)
                                                .collect(Collectors.groupingBy(d -> d.getCurrentFloor().getSite()))
                                                .entrySet().stream()
                                                .map(e -> {
                                                        SiteSummaryResponse s = new SiteSummaryResponse();
                                                        s.setSiteId(e.getKey().getSiteId());
                                                        s.setSiteName(e.getKey().getSiteName());
                                                        s.setTotal(e.getValue().size());
                                                        return s;
                                                }).toList();
                                break;
                }

                return StatusSummaryResponse.builder()
                                .type(typeList)
                                .model(modelList)
                                .site(siteList)
                                .build();
        }

        @Override
        public StatusSummaryResponse getStatusSummaryWithoutSerial(DeviceStatus status) {
                List<Site> sites = siteRepository.findAll();
                DeviceType[] types = DeviceType.values();

                List<TypeSummaryResponse> typeList = new ArrayList<>();
                List<ModelSummaryResponse> modelList = new ArrayList<>();
                List<SiteSummaryResponse> siteList = new ArrayList<>();

                // Tổng hợp theo type và model
                for (DeviceType t : types) {
                        int typeTotal = 0;

                        List<Model> models = modelRepository.findByType(t);
                        // Tổng hợp theo model
                        for (Model model : models) {
                                int modelTotal = 0;
                                switch (status) {
                                        case IN_STOCK -> {
                                                modelTotal += deviceWarehouseRepository.sumQuantityInStockBySite(t,
                                                                model.getModelId(), null);
                                        }
                                        case IN_FLOOR -> {
                                                int in = transactionDetailRepository.sumFloorInBySite(t,
                                                                model.getModelId(), null);
                                                int out = transactionDetailRepository.sumFloorOutBySite(t,
                                                                model.getModelId(), null);
                                                modelTotal += (in - out);
                                        }

                                        case ASSIGNED -> {
                                                int assignment = transactionDetailRepository.sumAssignment(null,
                                                                model.getModelId());
                                                int returnFromUser = transactionDetailRepository
                                                                .sumReturnFromUser(null, model.getModelId());
                                                modelTotal += assignment - returnFromUser;
                                        }
                                        case ON_THE_MOVE -> {
                                                modelTotal += transactionDetailRepository.sumOnTheMove(null,
                                                                model.getModelId(), null);
                                        }
                                        case REPAIR -> {
                                                int repairIn = transactionDetailRepository.sumReturnFromRepair(t,
                                                                model.getModelId());
                                                int repairOut = transactionDetailRepository
                                                                .sumRepair(t, model.getModelId());
                                                modelTotal += repairOut - repairIn;
                                        }
                                        case DISPOSED -> {
                                                modelTotal += transactionDetailRepository.sumDisposed(t,
                                                                model.getModelId(), null);
                                        }
                                        case E_WASTE -> {
                                                modelTotal += transactionDetailRepository.sumEWaste(t,
                                                                model.getModelId(), null);
                                        }
                                }
                                if (modelTotal <= 0) {
                                        continue; // Không có thiết bị nào
                                }
                                modelList.add(ModelSummaryResponse.builder()
                                                .modelId(model.getModelId())
                                                .modelName(model.getModelName())
                                                .total(modelTotal)
                                                .sites(null)
                                                .build());
                                typeTotal += modelTotal;
                        }
                        if (typeTotal <= 0) {
                                continue; // Không có thiết bị nào
                        }
                        typeList.add(TypeSummaryResponse.builder()
                                        .type(t)
                                        .total(typeTotal)
                                        .build());
                }

                // Tổng hợp theo site
                for (Site site : sites) {
                        Integer siteId = site.getSiteId();
                        int totalSite = 0;
                        switch (status) {
                                case IN_STOCK -> {
                                        totalSite += deviceWarehouseRepository.sumQuantityInStockBySite(null, null,
                                                        siteId);
                                }
                                case IN_FLOOR -> {
                                        int in = transactionDetailRepository.sumFloorInBySite(null, null, siteId);
                                        int out = transactionDetailRepository.sumFloorOutBySite(null, null, siteId);
                                        totalSite += (in - out);
                                }
                                case E_WASTE -> {
                                        int quantity = transactionDetailRepository.sumEWaste(null, null, siteId);
                                        totalSite += quantity;
                                }
                                default -> {
                                        // Không lấy theo site với các trạng thái khác
                                }
                        }
                        if (totalSite > 0) {
                                SiteSummaryResponse siteSummary = SiteSummaryResponse.builder()
                                                .siteId(siteId)
                                                .siteName(site.getSiteName())
                                                .total(totalSite)
                                                .build();
                                siteList.add(siteSummary);
                        }
                }

                return StatusSummaryResponse.builder()
                                .type(typeList)
                                .model(modelList)
                                .site(siteList) // Chưa tổng hợp theo site
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
