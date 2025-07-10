package com.concentrix.asset.dto.response;

import com.concentrix.asset.enums.TransferStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransferItemResponse implements Serializable {

   Integer deviceId;
   String serialNumber;
   String deviceName;
   Integer quantity;

}
