package com.concentrix.asset.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CompareDataResponse implements Serializable {

    LocalDate fromDate;
    LocalDate toDate;

    // Dữ liệu trending theo nhóm (ví dụ: site1: [count_from, count_to], site2: [count_from, count_to])
    Map<String, List<Integer>> datasets;
}