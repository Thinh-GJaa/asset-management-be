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
public class UserDetailResponse implements Serializable {
    String eid;
    String fullName;
    String email;
    String msa;
    String sso;
    String phoneNumber;
    String department;
    String position;
    // Thêm các trường khác nếu cần
}