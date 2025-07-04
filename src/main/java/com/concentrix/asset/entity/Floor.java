package com.concentrix.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Entity lưu thông tin các sàn làm việc (floor), thuộc về một site cụ thể.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Floor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer floorId;

    @Column
    String floorName;

    @ManyToOne
    @JoinColumn(name = "site_id")
    Site site;
}