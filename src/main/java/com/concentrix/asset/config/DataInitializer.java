package com.concentrix.asset.config;

import com.concentrix.asset.entity.User;
import com.concentrix.asset.enums.Role;
import com.concentrix.asset.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Value("${app.password.default-admin}")
    String defaultPwdAdmin;

    @Bean
    CommandLineRunner initAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEid = "admin";
            if (userRepository.findById(adminEid).isEmpty()) {
                User admin = User.builder()
                        .eid(adminEid)
                        .fullName("Administrator")
                        .jobTitle("Administrator Asset Management System")
                        .email("admin_ams@concentrix.com")
                        .password(passwordEncoder.encode(defaultPwdAdmin))
                        .role(Role.ADMIN)
                        .isActive(true)
                        .build();
                userRepository.save(admin);
            }
        };
    }
}
