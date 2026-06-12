package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "outbound_order_items")
public class OutboundOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long materialId;

    @Column(length = 30)
    private String materialCode;

    @Column(length = 100)
    private String materialName;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal quantity;

    @Column(length = 20)
    private String unit;

    @Column(precision = 12, scale = 2)
    private BigDecimal referencePrice;

    @Column(precision = 14, scale = 2)
    private BigDecimal amount;
}
