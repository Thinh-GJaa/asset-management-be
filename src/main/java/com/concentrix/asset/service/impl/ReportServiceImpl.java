package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.response.*;
import com.concentrix.asset.entity.Device;
import com.concentrix.asset.entity.Model;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.mapper.DeviceMapper;
import com.concentrix.asset.repository.*;
import com.concentrix.asset.service.ReportService;
import com.concentrix.asset.service.TypeService;
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
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ReportServiceImpl implements ReportService {
    SiteRepository siteRepository;
    FloorRepository floorRepository;
    DeviceRepository deviceRepository;
    ModelRepository modelRepository;
    DeviceWarehouseRepository deviceWarehouseRepository;
    TransactionDetailRepository transactionDetailRepository;
    DeviceMapper deviceMapper;
    TypeService typeService;
    DeviceFloorRepository deviceFloorRepository;
    DeviceUserRepository deviceUserRepository;

    @Override
    public Map<String, Map<String, Integer>> getStatusSummaryAllSite() {
        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();

        for (DeviceStatus status : DeviceStatus.values()) {
            int withSerial = deviceRepository.countByStatusAndSerialNumberIsNotNull(status);

            int withoutSerial = switch (status) {
                case IN_STOCK -> deviceWarehouseRepository.sumAllStock();

                case IN_FLOOR -> deviceFloorRepository.sumDeviceInFloor();

                case ON_THE_MOVE -> transactionDetailRepository.sumAllOnTheMove();

                case DISPOSED -> transactionDetailRepository.sumAllDisposal();

                case E_WASTE -> transactionDetailRepository.sumAllEWaste();

                case REPAIR -> transactionDetailRepository.sumAllRepair()
                        - transactionDetailRepository.sumAllReturnFromRepair();
                case ASSIGNED -> deviceUserRepository.sumDeviceAssigned();

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
        List<DeviceType> types = (type != null) ? List.of(type) : Arrays.asList(DeviceType.values());
        List<SiteDeviceWithoutSerialSummaryResponse> result = new ArrayList<>();

        switch (status) {
            case IN_STOCK, IN_FLOOR, E_WASTE -> {
                List<Site> sites = siteRepository.findAll();
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
                                case IN_STOCK -> quantity = deviceWarehouseRepository
                                        .sumStockBySite_Type_Model(site.getSiteId(), t, model.getModelId());

                                case IN_FLOOR -> quantity = deviceFloorRepository
                                        .sumDeviceBySite_Type_Model(site.getSiteId(), t, model.getModelId());

                                case E_WASTE -> quantity = transactionDetailRepository
                                        .sumEWasteBySite_Type_Model(site.getSiteId(), t, model.getModelId());

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

            }
            default -> {
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
                            case ASSIGNED -> quantity = deviceUserRepository
                                    .sumDeviceAssignedByType_Model(t, model.getModelId());
                            case ON_THE_MOVE -> quantity = transactionDetailRepository
                                    .sumOnTheMove(t, model.getModelId());
                            case DISPOSED -> quantity = transactionDetailRepository
                                    .sumDisposedByType_Model(t, model.getModelId());
                            case REPAIR -> {
                                int repairIn = transactionDetailRepository
                                        .sumReturnFromRepair(t, model.getModelId());
                                int repairOut = transactionDetailRepository
                                        .sumRepair(t, model.getModelId());
                                quantity = repairOut - repairIn;
                            }
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
                    siteSummary.setSiteId(null);
                    siteSummary.setSiteName("All Sites");
                    siteSummary.setTypes(typeSummaries);
                    result.add(siteSummary);
                }

            }
        }
        return result;
    }

    @Override
    public List<TypeSummaryResponse> getWithSerialSummary(Integer siteId, DeviceStatus status,
            Integer floorId, Integer ownerId, Integer accountId, DeviceType type, Integer modelId,
            Boolean isOutOfWarranty, String ageRange) {
        List<DeviceType> types = (type != null) ? List.of(type) : Arrays.asList(DeviceType.values());
        List<Site> sites = (siteId == null) ? siteRepository.findAll()
                : Collections.singletonList(siteRepository.findById(siteId).orElse(null));
        List<TypeSummaryResponse> typeSummaries = new ArrayList<>();

        LocalDate[] dateRange = getDateRangeFromAgeRange(ageRange);
        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];

        for (DeviceType t : types) {
            if (type != null && !t.equals(type)) {
                continue; // Bỏ qua nếu type là null
            }
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
                            if (siteId != null && !site.getSiteId().equals(siteId))
                                continue;
                            int quantity = deviceRepository.countAssetInStock(
                                    site.getSiteId(), t, model.getModelId(), isOutOfWarranty, startDate, endDate);
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
                            if (siteId != null && !site.getSiteId().equals(siteId))
                                continue;
                            int quantity = deviceRepository.countAssetInFloor(
                                    site.getSiteId(), ownerId, accountId, floorId, t, model.getModelId(),
                                    isOutOfWarranty, startDate, endDate);
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

                    case E_WASTE -> {
                        for (Site site : sites) {
                            if (siteId != null && !site.getSiteId().equals(siteId))
                                continue;
                            int quantity = deviceRepository.countAssetEWaste(
                                    site.getSiteId(), t, model.getModelId(), isOutOfWarranty, startDate, endDate);
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

                    default -> {
                        int quantity = deviceRepository.countAssetByStatus(status, t,
                                model.getModelId(), isOutOfWarranty, startDate, endDate);
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
                    if (status == DeviceStatus.IN_STOCK || status == DeviceStatus.IN_FLOOR
                            || status == DeviceStatus.E_WASTE) {
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
            Integer ownerId, Integer accountId, DeviceType type, Integer modelId, Boolean isOutOfWarranty,
            String ageRange) {

        LocalDate[] dateRange = getDateRangeFromAgeRange(ageRange);
        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];

        List<Device> devices = new ArrayList<>();

        // Nếu status = null, lấy tất cả devices với các filter khác
        if (status == null) {
            devices = deviceRepository.findAllDevicesForReport(siteId, floorId, ownerId, accountId, type, modelId,
                    isOutOfWarranty, startDate, endDate);
        } else {
            switch (status) {
                case IN_STOCK ->
                    devices = deviceRepository.findDevicesInStockForReport(siteId, type, modelId, isOutOfWarranty,
                            startDate, endDate);

                case IN_FLOOR ->
                    devices = deviceRepository.findDevicesInFloorForReport(siteId, floorId, ownerId, accountId, type,
                            modelId, isOutOfWarranty, startDate, endDate);

                case E_WASTE ->
                    devices = deviceRepository.findDevicesEWasteForReport(siteId, type, modelId, isOutOfWarranty,
                            startDate,
                            endDate);

                default ->
                    devices = deviceRepository.findDevicesStatusForReport(status, type, modelId, isOutOfWarranty,
                            startDate,
                            endDate);
            }
        }

        return devices.stream()
                .map(deviceMapper::toDeviceResponse)
                .collect(Collectors.toList());

    }

    @Override
    public List<SiteTypeChartResponse> getSiteTypeChartWithSerial(DeviceStatus status) {
        // Định nghĩa main type có serial
        Set<DeviceType> mainTypes = Set.of(
                DeviceType.MONITOR, DeviceType.DESKTOP, DeviceType.LAPTOP,
                DeviceType.IMAC, DeviceType.MACBOOK, DeviceType.MAC_MINI);
        List<SiteTypeChartResponse> result = new ArrayList<>();

        List<Site> sites = siteRepository.findAll();
        List<Device> devices = deviceRepository.findAllBySerialNumberIsNotNull();

        switch (status) {
            case IN_STOCK, IN_FLOOR, E_WASTE -> {
                for (Site site : sites) {
                    List<SiteTypeChartResponse.TypeCount> typeCounts = new ArrayList<>();
                    int otherCount = 0;
                    for (DeviceType type : mainTypes) {
                        int count = (int) devices.parallelStream()
                                .filter(d -> d.getModel().getType() == type
                                        && d.getStatus() == status
                                        && ((d.getCurrentWarehouse() != null
                                                && d.getCurrentWarehouse()
                                                        .getSite()
                                                        .getSiteId()
                                                        .equals(site.getSiteId()))
                                                || (d.getCurrentFloor() != null
                                                        && d.getCurrentFloor()
                                                                .getSite()
                                                                .getSiteId()
                                                                .equals(site.getSiteId()))))
                                .count();
                        typeCounts.add(SiteTypeChartResponse.TypeCount.builder()
                                .type(type.name())
                                .count(count)
                                .build());
                    }
                    // Đếm OTHER
                    otherCount = (int) devices.stream()
                            .filter(d -> !mainTypes.contains(d.getModel().getType())
                                    && d.getStatus() == status
                                    && ((d.getCurrentWarehouse() != null
                                            && d.getCurrentWarehouse()
                                                    .getSite()
                                                    .getSiteId()
                                                    .equals(site.getSiteId()))
                                            || (d.getCurrentFloor() != null
                                                    && d
                                                            .getCurrentFloor()
                                                            .getSite()
                                                            .getSiteId()
                                                            .equals(site.getSiteId()))))
                            .count();
                    typeCounts.add(SiteTypeChartResponse.TypeCount.builder()
                            .type("OTHER")
                            .count(otherCount)
                            .build());
                    result.add(SiteTypeChartResponse.builder()
                            .siteId(site.getSiteId())
                            .siteName(site.getSiteName())
                            .typeCounts(typeCounts)
                            .build());
                }
            }
            default -> {
                List<SiteTypeChartResponse.TypeCount> typeCounts = new ArrayList<>();

                for (DeviceType type : mainTypes) {
                    int count = (int) devices.stream()
                            .filter(d -> d.getModel().getType() == type
                                    && d.getStatus() == status)
                            .count();
                    typeCounts.add(SiteTypeChartResponse.TypeCount.builder()
                            .type(type.name())
                            .count(count)
                            .build());

                }
                // Đếm OTHER
                int otherCount = (int) devices.stream()
                        .filter(d -> !mainTypes.contains(d.getModel().getType())
                                && d.getStatus() == status)
                        .count();
                typeCounts.add(SiteTypeChartResponse.TypeCount.builder()
                        .type("OTHER")
                        .count(otherCount)
                        .build());

                result.add(SiteTypeChartResponse.builder()
                        .siteId(null) // Không phân theo site với các trạng thái này
                        .siteName("All Sites")
                        .typeCounts(typeCounts)
                        .build());
            }

        }

        return result;
    }

    @Override
    public List<SiteTypeChartResponse> getSiteTypeChartWithoutSerial(DeviceStatus status) {
        Set<DeviceType> mainTypes = Set.of(
                DeviceType.MOUSE, DeviceType.KEYBOARD, DeviceType.HEADSET,
                DeviceType.DONGLE, DeviceType.UBIKEY);
        List<DeviceType> allTypes = typeService.getTypeWithoutSerial();
        List<Site> sites = siteRepository.findAll();

        List<SiteTypeChartResponse> result = new ArrayList<>();

        boolean isGlobalStatus = status == DeviceStatus.DISPOSED ||
                status == DeviceStatus.ASSIGNED ||
                status == DeviceStatus.REPAIR ||
                status == DeviceStatus.ON_THE_MOVE;

        if (isGlobalStatus) {
            List<SiteTypeChartResponse.TypeCount> typeCounts = buildTypeCounts(mainTypes, status, null);
            int otherCount = buildTypeCounts(filterOtherTypes(allTypes, mainTypes), status, null)
                    .stream()
                    .mapToInt(SiteTypeChartResponse.TypeCount::getCount)
                    .sum();

            typeCounts.add(SiteTypeChartResponse.TypeCount.builder()
                    .type("OTHER")
                    .count(otherCount)
                    .build());

            result.add(SiteTypeChartResponse.builder()
                    .siteId(null)
                    .siteName("All Sites")
                    .typeCounts(typeCounts)
                    .build());

            return result;
        }

        // Trường hợp phân theo site
        for (Site site : sites) {
            Integer siteId = site.getSiteId();
            List<SiteTypeChartResponse.TypeCount> typeCounts = buildTypeCounts(mainTypes, status, siteId);
            int otherCount = buildTypeCounts(filterOtherTypes(allTypes, mainTypes), status, siteId)
                    .stream()
                    .mapToInt(SiteTypeChartResponse.TypeCount::getCount)
                    .sum();

            typeCounts.add(SiteTypeChartResponse.TypeCount.builder()
                    .type("OTHER")
                    .count(otherCount)
                    .build());

            result.add(SiteTypeChartResponse.builder()
                    .siteId(siteId)
                    .siteName(site.getSiteName())
                    .typeCounts(typeCounts)
                    .build());
        }

        return result;
    }

    private List<DeviceType> filterOtherTypes(List<DeviceType> all, Set<DeviceType> exclude) {
        return all.stream()
                .filter(type -> !exclude.contains(type))
                .toList();
    }

    private List<SiteTypeChartResponse.TypeCount> buildTypeCounts(Collection<DeviceType> types,
            DeviceStatus status,
            Integer siteId) {
        List<SiteTypeChartResponse.TypeCount> result = new ArrayList<>();
        for (DeviceType type : types) {
            int count = getDeviceCountByStatus(status, type, siteId);
            result.add(SiteTypeChartResponse.TypeCount.builder()
                    .type(type.name())
                    .count(count)
                    .build());
        }
        return result;
    }

    private int getDeviceCountByStatus(DeviceStatus status, DeviceType type, Integer siteId) {
        return switch (status) {
            case DISPOSED -> transactionDetailRepository.sumDisposedByType_Model(type, null);
            case ASSIGNED -> deviceUserRepository.sumDeviceAssignedByType_Model(type, null);
            case REPAIR -> {
                int repairIn = transactionDetailRepository.sumReturnFromRepair(type, null);
                int repairOut = transactionDetailRepository.sumRepair(type, null);
                yield repairOut - repairIn;
            }
            case ON_THE_MOVE -> transactionDetailRepository.sumOnTheMove(type, null);
            case IN_STOCK -> deviceWarehouseRepository.sumStockBySite_Type_Model(siteId, type, null);
            case IN_FLOOR -> deviceFloorRepository.sumDeviceBySite_Type_Model(siteId, type, null);
            case E_WASTE -> transactionDetailRepository.sumEWasteBySite_Type_Model(siteId, type, null);
        };
    }

    public LocalDate[] getDateRangeFromAgeRange(String ageRange) {
        LocalDate startDate = null;
        LocalDate endDate = null;

        if (ageRange == null) {
            return new LocalDate[] { startDate, endDate };
        }

        switch (ageRange) {
            case "<=1" -> startDate = LocalDate.now().minusYears(1);
            case "1-2" -> {
                startDate = LocalDate.now().minusYears(2);
                endDate = LocalDate.now().minusYears(1);
            }
            case "2-3" -> {
                startDate = LocalDate.now().minusYears(3);
                endDate = LocalDate.now().minusYears(2);
            }
            case "3-4" -> {
                startDate = LocalDate.now().minusYears(4);
                endDate = LocalDate.now().minusYears(3);
            }
            case "4-5" -> {
                startDate = LocalDate.now().minusYears(5);
                endDate = LocalDate.now().minusYears(4);
            }
            case "5-6" -> {
                startDate = LocalDate.now().minusYears(6);
                endDate = LocalDate.now().minusYears(5);
            }
            case ">6" -> endDate = LocalDate.now().minusYears(6);
        }

        return new LocalDate[] { startDate, endDate };
    }

    @Override
    public byte[] generateDeviceListCsv(Integer siteId, DeviceStatus status, Integer floorId, Integer ownerId,
            Integer accountId, DeviceType type, Integer modelId, Boolean isOutOfWarranty, String ageRange) {
        List<DeviceResponse> devices = getDeviceListForReport(siteId, status, floorId, ownerId, accountId, type,
                modelId, isOutOfWarranty, ageRange);

        StringBuilder csvContent = new StringBuilder();

        // CSV Header
        csvContent.append("Device ID,Serial Number,Device Name,Host Name,Seat Number,PO ID,Purchase Date,")
                .append("Model ID,Model Name,Device Type,")
                .append("User EID,User Full Name,")
                .append("Floor ID,Floor Name,")
                .append("Warehouse ID,Warehouse Name,")
                .append("Site ID,Site Name,")
                .append("Status,Description\n");

        // CSV Data
        for (DeviceResponse device : devices) {
            csvContent.append(escapeCsvValue(device.getDeviceId())).append(",")
                    .append(escapeCsvValue(device.getSerialNumber())).append(",")
                    .append(escapeCsvValue(device.getDeviceName())).append(",")
                    .append(escapeCsvValue(device.getHostName())).append(",")
                    .append(escapeCsvValue(device.getSeatNumber())).append(",")
                    .append(escapeCsvValue(device.getPoId())).append(",")
                    .append(escapeCsvValue(device.getPurchaseDate() != null ? device.getPurchaseDate().toString() : ""))
                    .append(",")
                    .append(escapeCsvValue(device.getModel() != null ? device.getModel().getModelId() : "")).append(",")
                    .append(escapeCsvValue(device.getModel() != null ? device.getModel().getModelName() : ""))
                    .append(",")
                    .append(escapeCsvValue(device.getModel() != null ? device.getModel().getType() : "")).append(",")
                    .append(escapeCsvValue(device.getUser() != null ? device.getUser().getEid() : "")).append(",")
                    .append(escapeCsvValue(device.getUser() != null ? device.getUser().getFullName() : "")).append(",")
                    .append(escapeCsvValue(device.getFloor() != null ? device.getFloor().getFloorId() : "")).append(",")
                    .append(escapeCsvValue(device.getFloor() != null ? device.getFloor().getFloorName() : ""))
                    .append(",")
                    .append(escapeCsvValue(device.getWarehouse() != null ? device.getWarehouse().getWarehouseId() : ""))
                    .append(",")
                    .append(escapeCsvValue(
                            device.getWarehouse() != null ? device.getWarehouse().getWarehouseName() : ""))
                    .append(",")
                    .append(escapeCsvValue(device.getSite() != null ? device.getSite().getSiteId() : "")).append(",")
                    .append(escapeCsvValue(device.getSite() != null ? device.getSite().getSiteName() : "")).append(",")
                    .append(escapeCsvValue(device.getStatus())).append(",")
                    .append(escapeCsvValue(device.getDescription())).append("\n");
        }

        return csvContent.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escapeCsvValue(Object value) {
        if (value == null) {
            return "";
        }
        String stringValue = value.toString();
        if (stringValue.contains(",") || stringValue.contains("\"") || stringValue.contains("\n")) {
            return "\"" + stringValue.replace("\"", "\"\"") + "\"";
        }
        return stringValue;
    }

}
