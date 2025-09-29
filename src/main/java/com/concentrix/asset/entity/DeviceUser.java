package com.concentrix.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@IdClass(DeviceUserId.class)
public class DeviceUser {
    @Id
    @ManyToOne
    @JoinColumn(name = "eid")
    User user;

    @Id
    @ManyToOne
    @JoinColumn(name = "device_id")
    Device device;

    @Column
    Integer quantity;

}