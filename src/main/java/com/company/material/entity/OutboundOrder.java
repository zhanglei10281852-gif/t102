package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "outbound_orders")
public class OutboundOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String orderNo;

    @Column(nullable = false, length = 20)
    private String outboundType;

    @Column(nullable = false, length = 50)
    private String department;

    @Column(nullable = false, length = 50)
    private String applicant;

    @Column(nullable = false)
    private Long targetWarehouseId;

    @Column(length = 300)
    private String purpose;

    @Column(nullable = false)
    private LocalDate outboundDate;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 300)
    private String rejectReason;

    private Long approvedBy;
    private LocalDateTime approvedAt;

    private Long postedBy;
    private LocalDateTime postedAt;

    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "待审批";
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
