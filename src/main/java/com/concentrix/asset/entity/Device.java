package com.concentrix.asset.entity;

import com.concentrix.asset.enums.DeviceStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

/**
 * Entity lưu thông tin từng thiết bị cụ thể (serial, trạng thái, model, vị trí
 * hiện tại).
 */
@Getter
@Setter
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

    @Column
    String hostName;

    @Column(unique = true)
    String seatNumber;

    @Enumerated(EnumType.STRING)
    DeviceStatus status;

    @Column
    LocalDate startDate; //Thời gian bắt đầu tính bảo hành

    @Column
    LocalDate endDate; //Thời gian kết thúc bảo hành

    @ManyToOne
    @JoinColumn(name = "model_id")
    Model model;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    Warehouse currentWarehouse;

    @ManyToOne
    @JoinColumn(name = "floor_id")
    Floor currentFloor;

    @ManyToOne
    @JoinColumn(name = "user_id")
    User currentUser;

    @OneToMany(mappedBy = "device")
    List<DeviceWarehouse> deviceWarehouses;

    @OneToMany(mappedBy = "device")
    List<DeviceUser> deviceUsers;

    @OneToMany(mappedBy = "device")
    List<DeviceFloor> deviceFloors;

    @OneToMany(mappedBy = "device")
    List<PODetail> poDetails;

}