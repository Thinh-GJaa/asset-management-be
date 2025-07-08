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
public class FloorResponse implements Serializable {

    Integer floorId;
    String floorName;
    String description;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    SiteResponse site;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SiteResponse implements Serializable {
        Integer siteId;
        String siteName;
    }
}