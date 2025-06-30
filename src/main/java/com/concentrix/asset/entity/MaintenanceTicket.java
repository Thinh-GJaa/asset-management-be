package com.concentrix.asset.entity;

import com.concentrix.asset.enums.MaintenanceType;
import com.concentrix.asset.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "maintenance_ticket")
public class MaintenanceTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    Device device;

    @Column(nullable = false)
    LocalDateTime createdDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    TicketStatus status; // PENDING, DONE, CANCELLED

    @Column(length = 255)
    String note;

    // Người tạo phiếu bảo trì
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    Account createdBy;

    // Người duyệt phiếu bảo trì
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    Account approvedBy;

    @Column
    LocalDateTime approvedDate;

    // Liên kết với phiếu xuất kho (nếu đã xuất kho để bảo trì)
    @OneToOne(mappedBy = "maintenanceTicket", fetch = FetchType.LAZY)
    ExportTicket exportTicket;

    // Loại bảo trì
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    MaintenanceType maintenanceType; // PREVENTIVE, CORRECTIVE, EMERGENCY

    // Ngày dự kiến bảo trì
    @Column
    LocalDateTime scheduledDate;

    // Ngày hoàn thành bảo trì
    @Column
    LocalDateTime completedDate;
}
