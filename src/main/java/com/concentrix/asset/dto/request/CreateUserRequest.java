package com.concentrix.asset.dto.request;

import com.concentrix.asset.enums.Role;
import com.concentrix.asset.validator.ValidPassword;
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

    @NotBlank(message = "Email is required")
    String email;

    @NotBlank(message = "SSO is required")
    String SSO;

    @NotBlank(message = "MSA is required")
    String MSA;

    @NotBlank(message = "Job title is required")
    String jobTitle;

    @ValidPassword
    String password;

    Role role;


}