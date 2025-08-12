package com.concentrix.asset.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class POItemResponse implements Serializable {
    Integer deviceId;
    String deviceName;

    String serialNumber;
    Integer quantity;

}
