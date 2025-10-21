package com.concentrix.asset.thirdparty.lenovogateway;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "lenovo-api-client", url = "https://pcsupport.lenovo.com/vn/en/api/v4/mse")
public interface LenovoApiClient {
    @GetMapping("/getserial")
    List<LenovoProductResponse> getProductBySerialNumber(@RequestParam("productId") String serialNumber);
}
