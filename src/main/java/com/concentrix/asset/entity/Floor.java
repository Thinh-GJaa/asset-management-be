package com.concentrix.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity lưu thông tin các sàn làm việc (floor), thuộc về một site cụ thể.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Floor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer floorId;

    @Column
    String floorName;

    @ManyToOne
    @JoinColumn(name = "site_id")
    Site site;

    @ManyToOne
    @JoinColumn(name = "account_id")
    Account account;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    User createdBy;

    @Column
    String description;

    @Column
    LocalDateTime createdAt;

    @Column
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "floor")
    List<DeviceFloor> deviceFloors;

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