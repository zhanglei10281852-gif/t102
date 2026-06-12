package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "return_orders")
public class ReturnOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String returnNo;

    @Column(nullable = false)
    private Long originalOrderId;

    @Column(length = 20)
    private String originalOrderNo;

    @Column(nullable = false, length = 50)
    private String department;

    @Column(nullable = false, length = 50)
    private String applicant;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false)
    private LocalDate returnDate;

    @Column(nullable = false, length = 10)
    private String status;

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
