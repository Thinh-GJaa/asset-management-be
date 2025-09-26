package com.concentrix.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

/**
 * Entity đại diện cho chi tiết phiếu giao dịch (transaction detail).
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(TransactionDetailId.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionDetail implements Serializable {
    @Id
    @ManyToOne
    @JoinColumn(name = "transaction_id")
    AssetTransaction transaction;

    @Id
    @ManyToOne
    @JoinColumn(name = "device_id")
    Device device;

    @Column
    Integer quantity;
}