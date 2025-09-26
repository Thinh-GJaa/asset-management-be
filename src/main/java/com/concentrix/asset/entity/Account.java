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
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer accountId;

    @Column(unique = true)
    String accountName;

    @Column
    String accountCode;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    User owner;

    @Column
    String description;

    @Column
    LocalDateTime createdAt;

    @Column
    LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "create_by")
    User createdBy;

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