package com.concentrix.asset.thirdparty.lenovogateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LenovoProductResponse {
    @JsonProperty("Id")
    String id;
    @JsonProperty("Guid")
    String guid;
    @JsonProperty("Brand")
    String brand;
    @JsonProperty("Name")
    String name;
    @JsonProperty("Serial")
    String serial;
    @JsonProperty("Type")
    String type;
    @JsonProperty("ParentID")
    List<String> parentId;
    @JsonProperty("Image")
    String image;
    @JsonProperty("Popularity")
    String popularity;
    @JsonProperty("IsChina")
    int isChina;
    @JsonProperty("IsSupported")
    boolean isSupported;
    @JsonProperty("IsSolutionParent")
    boolean isSolutionParent;
    @JsonProperty("ProductBrand")
    String productBrand;
    @JsonProperty("Manufacturer")
    String manufacturer;
    @JsonProperty("MvsInfo")
    Object mvsInfo;
}
