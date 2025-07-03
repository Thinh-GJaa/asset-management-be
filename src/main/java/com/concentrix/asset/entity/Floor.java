package com.concentrix.asset.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Entity lưu thông tin các sàn làm việc (floor), thuộc về một site cụ thể.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
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