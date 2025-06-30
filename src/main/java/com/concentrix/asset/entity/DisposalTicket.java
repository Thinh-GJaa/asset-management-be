package com.concentrix.asset.entity;

import com.concentrix.asset.enums.DisposalMethod;
import com.concentrix.asset.enums.DisposalReason;
import com.concentrix.asset.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "disposal_ticket")
public class DisposalTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
     Device device;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    TicketStatus status; // PENDING, DONE, CANCELLED

    @Column(length = 255)
     String note;

    // Người tạo phiếu thanh lý
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
     Account account;

    // Người duyệt phiếu thanh lý
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
     Account approvedBy;

    @Column
     LocalDateTime approvedDate;

    // Liên kết với phiếu xuất kho (nếu đã xuất kho để thanh lý)
    @OneToOne(mappedBy = "disposalTicket", fetch = FetchType.LAZY)
     ExportTicket exportTicket;

    // Lý do thanh lý
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    DisposalReason disposalReason; // HONG_HONG, CU_KY, KHONG_SU_DUNG, NANG_CAP

    // Ngày dự kiến thanh lý
    @Column
     LocalDateTime scheduledDate;

    // Ngày hoàn thành thanh lý
    @Column
     LocalDateTime completedDate;

    // Phương thức thanh lý
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    DisposalMethod disposalMethod; // BAN, VUT_BO, TAI_CHE, CHUYEN_GIAO


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

