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
    String SSO;
    String MSA;
    String jobTitle;
    Role role;
    boolean isVerified;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}