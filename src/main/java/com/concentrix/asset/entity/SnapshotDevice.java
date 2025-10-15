package com.concentrix.asset.entity;

import com.concentrix.asset.enums.DeviceStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "snapshot_device")
@IdClass(SnapshotDeviceId.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SnapshotDevice {

    @Id
    @ManyToOne
    @JoinColumn(name = "device_id", nullable = false)
    Device device;

    @Id
    @Column(nullable = false)
    LocalDate snapshotDate;

    @ManyToOne
    @JoinColumn(name = "site_id")
    Site site;

    @Enumerated(EnumType.STRING)
    DeviceStatus status;
}
