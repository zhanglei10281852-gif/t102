package com.company.material.repository;

import com.company.material.entity.ReturnOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReturnOrderItemRepository extends JpaRepository<ReturnOrderItem, Long> {
    List<ReturnOrderItem> findByReturnOrderId(Long returnOrderId);
    void deleteByReturnOrderId(Long returnOrderId);
}
