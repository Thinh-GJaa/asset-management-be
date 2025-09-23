package com.concentrix.asset.dto.request;

import com.concentrix.asset.enums.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateUserRequest {

    @NotBlank(message = "EID is required")
    String eid;

    @NotBlank(message = "Full name is required")
    String fullName;

    @NotBlank(message = "SSO is required")
    String sso;

    String email;

    String msa;

    String jobTitle;

    Role role;

    Integer accountId;


}