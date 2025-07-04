package com.concentrix.asset.entity;

import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.enums.ModelSize;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

/**
 * Entity lưu thông tin các model thiết bị (ví dụ: tên model, loại thiết bị,
 * hãng sản xuất).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Model {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        Integer modelId;

        @Column(nullable = false, unique = true)
        String modelName;

        @Column
        String manufacturer;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        DeviceType type;

        @Column
        String description;

        @Column
        LocalDateTime createdAt;

        @Column
        LocalDateTime updatedAt;

        @PrePersist
        protected void onCreate() {
                createdAt = LocalDateTime.now();
                updatedAt = LocalDateTime.now();
        }

        @PreUpdate
        protected void onUpdate() {
                updatedAt = LocalDateTime.now();
        }
}