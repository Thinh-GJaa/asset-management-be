package com.concentrix.asset.dto.response;

import com.concentrix.asset.enums.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse implements Serializable {
    String eid;
    String fullName;
    String email;
    String sso;
    String msa;
    String jobTitle;
    Role role;
    String location;
    String company;
    String costCenter;
    String msaClient;
    String managerEmail;
    SiteResponse site;
    boolean isActive;
    AccountResponse account;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AccountResponse implements Serializable {
        Integer accountId;
        String accountName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SiteResponse implements Serializable {
        Integer siteId;
        String siteName;
    }
}