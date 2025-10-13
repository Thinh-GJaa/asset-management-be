package com.concentrix.asset.entity;

import com.concentrix.asset.enums.DeviceStatus;
import com.concentrix.asset.enums.DeviceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity đại diện cho phiếu giao dịch (transaction slip/header).
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "snapshot_device")
public class SnapshotDevice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @JoinColumn(name = "device_id")
    @ManyToOne
    Device device;

    @Column(nullable = false)
    LocalDate snapshotDate;

    @ManyToOne
    @JoinColumn(name = "site_id")
    Site site;

    @Enumerated(EnumType.STRING)
    DeviceStatus status;

}