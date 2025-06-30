package com.concentrix.asset.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@IdClass(DeviceUseId.class)
@Entity
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Table(name = "device_use")
public class DeviceUse {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    Device device;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "use_id", nullable = false)
    User uses;

    @Column(nullable = false)
    Integer quantity;

    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(nullable = false)
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
