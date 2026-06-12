package com.company.material.repository;

import com.company.material.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByWarehouseIdAndMaterialId(Long warehouseId, Long materialId);
    List<InventoryTransaction> findByRelatedOrderNo(String relatedOrderNo);

    @Query("SELECT COALESCE(SUM(t.totalCost), 0) FROM InventoryTransaction t " +
           "WHERE t.transactionType = '出库' AND t.materialId = :materialId " +
           "AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumOutboundCostByMaterialAndPeriod(@Param("materialId") Long materialId,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(t.totalCost), 0) FROM InventoryTransaction t " +
           "WHERE t.transactionType = '出库' AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumOutboundCostByPeriod(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    @Query("SELECT t.materialId, COALESCE(SUM(t.quantity), 0), COALESCE(SUM(t.totalCost), 0) " +
           "FROM InventoryTransaction t WHERE t.transactionType = '出库' " +
           "AND t.createdAt BETWEEN :start AND :end GROUP BY t.materialId ORDER BY SUM(t.quantity) DESC")
    List<Object[]> rankMaterialOutboundQuantity(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);
}
