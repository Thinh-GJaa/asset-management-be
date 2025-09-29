package com.concentrix.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity lưu thông tin phiếu đặt hàng (Purchase Order - P.O).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PurchaseOrder {

    @Id
    @Column(name = "po_id", unique = true, nullable = false)
    String poId;

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    Vendor vendor;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "created_by")
    User createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PODetail> poDetails = new ArrayList<>();

    @Column
    String note;

    @Column
    LocalDate createdAt;

    @Column
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}