package com.concentrix.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Entity lưu thông tin nhà cung cấp thiết bị (Vendor).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Vendor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer vendorId;

    @Column(nullable = false, unique = true)
    String vendorName;

    @Column
    String address;

    @Column
    String phoneNumber;

    @Column
    String email;

    @Column
    LocalDateTime createdAt;

    @Column
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}