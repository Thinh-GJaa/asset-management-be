package com.concentrix.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@IdClass(PODetailId.class)
public class PODetail {
    @Id
    @ManyToOne
    @JoinColumn(name = "po_id")
    PurchaseOrder purchaseOrder;

    @Id
    @ManyToOne
    @JoinColumn(name = "device_id")
    Device device;

    @Column
    Integer quantity;
}