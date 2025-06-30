package com.concentrix.asset.entity;

import com.concentrix.asset.enums.TicketStatus;
import com.concentrix.asset.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transaction")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    Device device;

    @Column(nullable = false)
    Integer quantity;

    @Column(name = "dateTransaction", nullable = false)
    LocalDateTime dateTransaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    TransactionType typetransaction; // IMPORT, EXPORT, TRANSFER, ADJUSTMENT

    // Liên kết với phiếu xuất kho (nếu có)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "export_ticket_id")
    ExportTicket exportTicket;

    // Liên kết với phiếu bảo trì (nếu có)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_ticket_id")
    MaintenanceTicket maintenanceTicket;

    // Liên kết với phiếu thanh lý (nếu có)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disposal_ticket_id")
    DisposalTicket disposalTicket;

    // Kho nguồn (cho giao dịch xuất/chuyển)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_stock_id")
    Stock fromStock;

    // Kho đích (cho giao dịch nhập/chuyển)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_stock_id")
    Stock toStock;

    // Người thực hiện giao dịch
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    Account performedBy;

    // Ghi chú giao dịch
    @Column(length = 255)
    String note;

    // Trạng thái giao dịch
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    TicketStatus status; // PENDING, COMPLETED, CANCELLED, FAILED
}
