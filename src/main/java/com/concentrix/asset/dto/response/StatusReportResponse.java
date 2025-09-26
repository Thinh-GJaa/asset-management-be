package com.concentrix.asset.dto.response;


import com.concentrix.asset.enums.DeviceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class StatusReportResponse {

    DeviceStatus status;
    Integer withSerialNumber;
    Integer withoutSerialNumber;

}
