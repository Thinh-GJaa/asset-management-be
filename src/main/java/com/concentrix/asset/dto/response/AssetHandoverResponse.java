package com.concentrix.asset.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetHandoverResponse {
    private Integer transactionId;
    private String itPerson;
    private String location;
    private String endUser;
    private String msa;
    private String employeeId;
    private String ssoEmail;
    private String assetType;
    private String issueDate;
    private String role;
    private LocalDateTime createdAt;
    private String createdBy;
    private List<AssetHandoverDetailResponse> assets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssetHandoverDetailResponse {
        private Integer deviceId;
        private String name;
        private String serialNumber;
        private Integer quantity;
        private String remark;
    }
} 