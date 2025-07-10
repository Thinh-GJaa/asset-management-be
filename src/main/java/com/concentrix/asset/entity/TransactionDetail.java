package com.concentrix.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import com.concentrix.asset.entity.AssetTransaction;

/**
 * Entity đại diện cho chi tiết phiếu giao dịch (transaction detail).
 */
@Entity
@IdClass(TransactionDetailId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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