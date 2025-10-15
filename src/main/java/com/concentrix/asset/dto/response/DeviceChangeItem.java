package com.concentrix.asset.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class DeviceChangeItem implements Serializable {

    String serialNumber;
    String deviceName;
    String modelName;
    String type;
    String status;
    String siteName;

}
