package com.concentrix.asset.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDate;

@Data // gồm equals + hashCode + getter/setter/toString
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SnapshotDeviceId implements Serializable {
    Integer device;        // phải trùng tên với field trong entity
    LocalDate snapshotDate;
}
