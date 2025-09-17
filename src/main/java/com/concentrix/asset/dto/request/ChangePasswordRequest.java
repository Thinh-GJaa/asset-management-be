package com.concentrix.asset.dto.request;


import com.concentrix.asset.validator.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;


@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    String currentPassword;

    @NotBlank(message = "New password is required")
            @ValidPassword
    String newPassword;

    @NotBlank(message = "Confirm password is required")
            @ValidPassword
    String confirmPassword;
}
