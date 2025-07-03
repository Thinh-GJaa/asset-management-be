package com.concentrix.asset.entity;

import com.concentrix.asset.enums.DeviceStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Entity lưu thông tin từng thiết bị cụ thể (serial, trạng thái, model, vị trí
 * hiện tại).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer deviceId;

    @Column(unique = true)
    String serialNumber;

    @Enumerated(EnumType.STRING)
    DeviceStatus currentStatus;

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
    @JoinColumn(name = "po_id")
    PurchaseOrder purchaseOrder;
}