package com.concentrix.asset.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Entity lưu thông tin phiếu đặt hàng (Purchase Order - P.O).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer poId;

    @Column(unique = true)
    String poNumber;

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    Vendor vendor;

    LocalDateTime createdAt;
    String createdBy;
    String note;
}