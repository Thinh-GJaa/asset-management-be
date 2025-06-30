package com.concentrix.asset.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateSiteRequest {

    @NotBlank(message = "Site name is required")
    String siteName;

    @NotBlank(message = "Site location is required")
    String siteLocation;
}
