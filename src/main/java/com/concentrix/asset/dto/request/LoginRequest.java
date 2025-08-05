package com.concentrix.asset.dto.request;

import com.concentrix.asset.validator.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class LoginRequest {

    @NotBlank(message = "Username is required")
    String email;

    @NotBlank(message = "Password is required")
    String password;
}