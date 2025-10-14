package com.concentrix.asset.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SnapshotDeviceId implements Serializable {
    Integer device;
    LocalDate snapshotDate;
}
