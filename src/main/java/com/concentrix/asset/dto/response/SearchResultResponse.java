package com.concentrix.asset.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchResultResponse implements Serializable {
    List<ResultItem> results;
    // Nếu chỉ có 1 kết quả, trả về detail luôn (deviceDetail hoặc userDetail)
    DeviceDetailResponse deviceDetail;
    UserDetailResponse userDetail;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ResultItem implements Serializable {
        String type; // DEVICE hoặc USER
        Integer deviceId;
        String deviceName;
        String serialNumber;
        String modelName;
        String status;
        String eid;
        String fullName;
        String email;
        String msa;
        String sso;
    }
}