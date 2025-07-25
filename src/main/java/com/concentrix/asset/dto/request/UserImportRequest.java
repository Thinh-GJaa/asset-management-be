package com.concentrix.asset.dto.request;

import lombok.Data;

@Data
public class UserImportRequest {
    private String eid;
    private String fullName;
    private String jobTitle;
    private String email;
    private String sso;
    private String msa;
    private String location;
    private String company;
    private String costCenter;
    private String msaClient;
    private String managerEmail;
    private Boolean isActive;
} 