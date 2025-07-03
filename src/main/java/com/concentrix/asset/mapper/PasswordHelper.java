package com.concentrix.asset.mapper;


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.mapstruct.Named;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PasswordHelper {

    PasswordEncoder passwordEncoder;

    @Named("hashPassword")
    public String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return null; // or throw an exception if you prefer
        }
        return passwordEncoder.encode(password);
    }
}
