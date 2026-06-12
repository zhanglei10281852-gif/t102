package com.company.material.repository;

import com.company.material.entity.ReturnOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ReturnOrderRepository extends JpaRepository<ReturnOrder, Long> {
    Optional<ReturnOrder> findByReturnNo(String returnNo);

    Page<ReturnOrder> findByStatus(String status, Pageable pageable);
    Page<ReturnOrder> findByDepartment(String department, Pageable pageable);

    @Query("SELECT MAX(r.returnNo) FROM ReturnOrder r WHERE r.returnNo LIKE :prefix")
    String findMaxReturnNoByPrefix(@Param("prefix") String prefix);

    Page<ReturnOrder> findByOriginalOrderId(Long originalOrderId, Pageable pageable);
}
