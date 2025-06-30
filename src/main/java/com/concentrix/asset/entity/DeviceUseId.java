package com.concentrix.asset.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class DeviceUseId implements Serializable {
    Long device;
    Long uses;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceUseId that = (DeviceUseId) o;
        return Objects.equals(device, that.device) && Objects.equals(uses, that.uses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(device, uses);
    }
}
