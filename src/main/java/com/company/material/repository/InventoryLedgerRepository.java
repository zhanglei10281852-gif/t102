package com.company.material.repository;

import com.company.material.entity.InventoryLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InventoryLedgerRepository extends JpaRepository<InventoryLedger, Long> {
    Optional<InventoryLedger> findByWarehouseIdAndMaterialId(Long warehouseId, Long materialId);
    List<InventoryLedger> findByWarehouseId(Long warehouseId);
    List<InventoryLedger> findByMaterialId(Long materialId);
    List<InventoryLedger> findByWarehouseIdAndMaterialIdIn(Long warehouseId, List<Long> materialIds);
}
