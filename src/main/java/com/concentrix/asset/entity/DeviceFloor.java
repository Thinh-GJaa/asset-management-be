package com.concentrix.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@IdClass(DeviceFloorId.class)
public class DeviceFloor {

    @Id
    @ManyToOne
    @JoinColumn(name = "floor_id")
    Floor floor;

    @Id
    @ManyToOne
    @JoinColumn(name = "device_id")
    Device device;

    @Column
    Integer quantity;

}