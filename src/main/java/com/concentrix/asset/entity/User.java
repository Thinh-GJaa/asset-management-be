package com.concentrix.asset.entity;

import com.concentrix.asset.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Entity lưu thông tin người dùng/nhân viên sử dụng thiết bị.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Id
    String eid;

    @Column(nullable = false)
    String fullName;

    @Column
    String jobTitle;

    @Column(unique = true)
    String email;

    @Column(unique = true)
    String SSO;

    @Column(unique = true)
    String MSA;

    @Column
    String password;

    @Column
    @Enumerated(EnumType.STRING)
    Role role;

    @Column
    LocalDateTime createdAt;

    @Column
    LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}