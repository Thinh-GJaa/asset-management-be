package com.concentrix.asset.service;

import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.entity.Device;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.entity.User;

import java.util.List;
import java.util.Map;

public interface RemindService {

    Map<User, Map<Device, Integer>> calculatePendingReturnsForAllUsers();

    Map<Site, List<AssetTransaction>> calculateHandoverImageReminder();
}