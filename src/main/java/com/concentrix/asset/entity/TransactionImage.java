package com.concentrix.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho phiếu giao dịch (transaction slip/header).
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "transaction_image")
public class TransactionImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer transactionImageId;

    @Column(unique = true, nullable = false)
    String imageName;

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    AssetTransaction assetTransaction;

    @Column
    LocalDateTime createdAt;

    @Column
    LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}