package com.concentrix.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Entity lưu thông tin các kho chứa thiết bị, thuộc về một site cụ thể.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer warehouseId;

    @Column
    String warehouseName;

    @ManyToOne
    @JoinColumn(name = "site_id")
    Site site;

    @Column
    String description;

    @Column
    LocalDateTime createdAt;

    @Column
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "warehouse")
    java.util.Set<DeviceWarehouse> deviceWarehouses;

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