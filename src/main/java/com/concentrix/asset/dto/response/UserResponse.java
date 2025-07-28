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
    boolean isActive;
    boolean isVerified;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}