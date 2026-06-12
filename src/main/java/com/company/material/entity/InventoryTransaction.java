package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false)
    private Long materialId;

    @Column(nullable = false, length = 20)
    private String transactionType;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal quantity;

    @Column(precision = 14, scale = 4)
    private BigDecimal unitCost;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalCost;

    @Column(length = 30)
    private String relatedOrderNo;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
