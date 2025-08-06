package com.concentrix.asset.dto.request;

import com.concentrix.asset.enums.Role;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserRequest {

    String eid;

    String email;
    String fullName;
    String password;
    String jobTitle;
    String sso;
    String msa;

    Role role;

}