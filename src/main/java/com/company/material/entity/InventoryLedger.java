package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventory_ledger", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"warehouseId", "materialId"})
})
public class InventoryLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false)
    private Long materialId;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(precision = 14, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.updatedAt = LocalDateTime.now();
        if (this.quantity == null) this.quantity = BigDecimal.ZERO;
        if (this.unitCost == null) this.unitCost = BigDecimal.ZERO;
        if (this.totalCost == null) this.totalCost = BigDecimal.ZERO;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
