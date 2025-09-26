package com.concentrix.asset.service.impl;

import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.service.TypeService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class TypeServiceImpl implements TypeService {

    @Override
    public List<DeviceType> getTypes() {
        return Arrays.stream(DeviceType.values())
                .sorted(Comparator.comparing(Enum::name)) // sắp xếp theo tên chữ cái
                .toList();
    }

    @Override
    public List<DeviceType> getTypeWithSerial() {
        return Arrays.stream(DeviceType.values())
                .filter(DeviceType::hasSerial)
                .sorted(Comparator.comparing(Enum::name))
                .toList();
    }

    @Override
    public List<DeviceType> getTypeWithoutSerial() {
        return Arrays.stream(DeviceType.values())
                .filter(type -> !type.hasSerial())
                .sorted(Comparator.comparing(Enum::name))
                .toList();
    }

}
