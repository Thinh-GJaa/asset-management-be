package com.concentrix.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Setter
@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "site")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long siteId;

    @Column(nullable = false, length = 25)
    String siteName;
    
    @Column(nullable = false, length = 50)
    String siteLocation;

    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(nullable = false)
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
