package com.company.material.repository;

import com.company.material.entity.OutboundOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboundOrderRepository extends JpaRepository<OutboundOrder, Long> {
    Optional<OutboundOrder> findByOrderNo(String orderNo);

    Page<OutboundOrder> findByStatus(String status, Pageable pageable);
    Page<OutboundOrder> findByDepartment(String department, Pageable pageable);
    Page<OutboundOrder> findByOutboundType(String outboundType, Pageable pageable);

    @Query("SELECT MAX(o.orderNo) FROM OutboundOrder o WHERE o.orderNo LIKE :prefix")
    String findMaxOrderNoByPrefix(@Param("prefix") String prefix);

    @Query("SELECT o FROM OutboundOrder o WHERE o.department = :dept AND o.status = '已出库' " +
           "AND o.postedAt BETWEEN :start AND :end")
    List<OutboundOrder> findPostedByDeptAndPeriod(@Param("dept") String dept,
                                                    @Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);

    @Query("SELECT o.department, COALESCE(SUM(o.totalAmount), 0) FROM OutboundOrder o " +
           "WHERE o.status = '已出库' AND o.postedAt BETWEEN :start AND :end " +
           "GROUP BY o.department ORDER BY SUM(o.totalAmount) DESC")
    List<Object[]> rankDepartmentAmount(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OutboundOrder o " +
           "WHERE o.status = '已出库' AND o.postedAt BETWEEN :start AND :end")
    BigDecimal sumPostedAmountByPeriod(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    @Query("SELECT o.outboundType, COUNT(o) FROM OutboundOrder o " +
           "WHERE o.createdAt BETWEEN :start AND :end GROUP BY o.outboundType")
    List<Object[]> countByTypeAndPeriod(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    long countByStatusAndCreatedAtBetween(String status, LocalDateTime start, LocalDateTime end);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT o.department, FUNCTION('DATE_FORMAT', o.postedAt, '%Y-%m'), COALESCE(SUM(o.totalAmount), 0) " +
           "FROM OutboundOrder o WHERE o.status = '已出库' AND o.postedAt BETWEEN :start AND :end " +
           "GROUP BY o.department, FUNCTION('DATE_FORMAT', o.postedAt, '%Y-%m') " +
           "ORDER BY o.department, FUNCTION('DATE_FORMAT', o.postedAt, '%Y-%m')")
    List<Object[]> deptMonthlyAmount(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);
}
