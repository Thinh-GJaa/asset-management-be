package com.concentrix.asset.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchResultResponse implements Serializable {

    Integer total;
    List<DeviceResponse> devices;
    List<UserResponse> users;

}