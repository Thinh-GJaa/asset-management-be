package com.concentrix.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import com.concentrix.asset.entity.AssetTransaction;

/**
 * Entity đại diện cho chi tiết phiếu giao dịch (transaction detail).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionDetail implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer detailId;

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    AssetTransaction transaction;

    @ManyToOne
    @JoinColumn(name = "device_id")
    Device device;

    @Column
    Integer quantity;

}