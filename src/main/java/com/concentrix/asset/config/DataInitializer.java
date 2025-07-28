package com.concentrix.asset.config;

import com.concentrix.asset.entity.User;
import com.concentrix.asset.enums.Role;
import com.concentrix.asset.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEid = "admin";
            if (userRepository.findById(adminEid).isEmpty()) {
                User admin = User.builder()
                        .eid(adminEid)
                        .fullName("Administrator")
                        .email("admin@example.com")
                        .password(passwordEncoder.encode("Admin@1234"))
                        .role(Role.ADMIN)
                        .isActive(true)
                        .build();
                userRepository.save(admin);
                System.out.println("Default admin user created: admin/admin123");
            }
        };
    }
}
