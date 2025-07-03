package com.concentrix.asset.dto.response;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SiteResponse implements Serializable{

    Long siteId;
    String siteName;
    String siteLocation;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

}
