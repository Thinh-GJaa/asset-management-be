package com.concentrix.asset.entity;

import com.concentrix.asset.enums.DeviceStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

/**
 * Entity lưu thông tin từng thiết bị cụ thể (serial, trạng thái, model, vị trí
 * hiện tại).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(indexes = {
        @Index(name = "idx_device_serial_number", columnList = "serialNumber"),
        @Index(name = "idx_device_status", columnList = "status"),
        @Index(name = "idx_device_model_id", columnList = "model_id"),
})
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer deviceId;

    @Column(unique = true)
    String serialNumber;

    @Column(nullable = false)
    String deviceName;

    @Enumerated(EnumType.STRING)
    DeviceStatus status;

    @ManyToOne
    @JoinColumn(name = "model_id")
    Model model;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    Warehouse currentWarehouse;

    @ManyToOne
    @JoinColumn(name = "floor_id", nullable = true)
    Floor currentFloor;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    User currentUser;

    @OneToMany(mappedBy = "device")
    Set<DeviceWarehouse> deviceWarehouses;
}