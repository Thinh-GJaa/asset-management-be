package com.concentrix.asset.service;


import com.concentrix.asset.enums.DeviceType;

import java.util.List;

public interface TypeService {

    List<DeviceType> getTypes();
    List<DeviceType> getTypeWithoutSerial();

    List<DeviceType> getTypeWithSerial();

}