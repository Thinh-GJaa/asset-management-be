package com.concentrix.asset.service;


import com.concentrix.asset.dto.response.LowStockResponse;

import java.util.List;

public interface LowStockService {

    List<LowStockResponse> getLowStockDevices();

}