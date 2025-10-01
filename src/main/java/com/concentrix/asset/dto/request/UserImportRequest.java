package com.concentrix.asset.dto.request;

import com.concentrix.asset.enums.Role;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UserImportRequest {
    String eid;
    String fullName;
    String jobTitle;
    String email;
    String sso;
    String msa;
    String location;
    String company;
    String costCenter;
    String msaClient;
    String msaProgram;
    String managerEmail;
    Boolean isActive;
    Role role;
} 