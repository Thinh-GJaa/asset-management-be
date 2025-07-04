package com.concentrix.asset.entity;

import com.concentrix.asset.enums.TransactionStatus;
import com.concentrix.asset.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho phiếu giao dịch (transaction slip/header).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "asset_transaction")
public class AssetTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    TransactionType transactionType;

    @ManyToOne
    @JoinColumn(name = "to_warehouse_id")
    Warehouse toWarehouse;

    @ManyToOne
    @JoinColumn(name = "user_use_id")
    User UserUse;

    @ManyToOne
    @JoinColumn(name = "to_floor_id")
    Floor toFloor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    TransactionStatus transactionStatus;

    @Column(nullable = false)
    LocalDateTime createdAt;

    @Column
    LocalDateTime updatedAt;

    @Column(nullable = false)
    String createdBy;

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