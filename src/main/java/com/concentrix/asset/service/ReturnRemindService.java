package com.concentrix.asset.service;

import com.concentrix.asset.entity.Device;
import com.concentrix.asset.entity.User;

import java.util.Map;

public interface ReturnRemindService {

    Map<User, Map<Device, Integer>> calculatePendingReturnsForAllUsers();

}