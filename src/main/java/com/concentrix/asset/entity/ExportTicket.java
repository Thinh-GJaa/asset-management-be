package com.concentrix.asset.entity;

import com.concentrix.asset.enums.ExportType;
import com.concentrix.asset.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "export_ticket")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ExportTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id",  nullable = false)
    Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_stock_id", nullable = false)
    Stock fromStock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_stock_id")
    Stock toStock; // nullable nếu không chuyển site

    @Column(nullable = false)
    Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    ExportType type; // CAP_PHAT, BAO_TRI, THANH_LY, CHUYEN_SITE

    @Column(nullable = false)
    LocalDateTime createdDate;

    @Column(nullable = false)
    boolean approved; // true nếu admin đã duyệt

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    Account approvedBy;

    @Column
    LocalDateTime approvedDate;

    @Column(length = 255)
    String note;

    // Liên kết với phiếu bảo trì (nếu xuất kho để bảo trì)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_ticket_id")
    MaintenanceTicket maintenanceTicket;

    // Liên kết với phiếu thanh lý (nếu xuất kho để thanh lý)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disposal_ticket_id")
    DisposalTicket disposalTicket;


    // Trạng thái xuất kho
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    TicketStatus status; // PENDING, COMPLETED, CANCELLED

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
