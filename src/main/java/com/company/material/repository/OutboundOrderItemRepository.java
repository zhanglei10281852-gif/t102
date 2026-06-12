package com.company.material.repository;

import com.company.material.entity.OutboundOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboundOrderItemRepository extends JpaRepository<OutboundOrderItem, Long> {
    List<OutboundOrderItem> findByOrderId(Long orderId);
    List<OutboundOrderItem> findByOrderIdIn(List<Long> orderIds);
    void deleteByOrderId(Long orderId);
}
