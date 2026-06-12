package com.company.material.controller;

import com.company.material.entity.InventoryLedger;
import com.company.material.entity.InventoryTransaction;
import com.company.material.entity.Material;
import com.company.material.repository.InventoryLedgerRepository;
import com.company.material.repository.InventoryTransactionRepository;
import com.company.material.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryLedgerRepository inventoryLedgerRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final MaterialRepository materialRepository;

    @GetMapping("/ledger")
    public ResponseEntity<?> listLedger(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long materialId) {
        List<InventoryLedger> ledgers;
        if (warehouseId != null && materialId != null) {
            ledgers = inventoryLedgerRepository.findByWarehouseIdAndMaterialId(warehouseId, materialId)
                    .map(List::of).orElse(List.of());
        } else if (warehouseId != null) {
            ledgers = inventoryLedgerRepository.findByWarehouseId(warehouseId);
        } else if (materialId != null) {
            ledgers = inventoryLedgerRepository.findByMaterialId(materialId);
        } else {
            ledgers = inventoryLedgerRepository.findAll();
        }
        return ResponseEntity.ok(ledgers);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> listTransactions(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) String orderNo) {
        if (orderNo != null && !orderNo.isBlank()) {
            return ResponseEntity.ok(inventoryTransactionRepository.findByRelatedOrderNo(orderNo));
        }
        if (warehouseId != null && materialId != null) {
            return ResponseEntity.ok(inventoryTransactionRepository.findByWarehouseIdAndMaterialId(warehouseId, materialId));
        }
        return ResponseEntity.ok(inventoryTransactionRepository.findAll());
    }

    @PostMapping("/init")
    public ResponseEntity<?> initInventory(@RequestBody Map<String, Object> body) {
        try {
            Long warehouseId = Long.valueOf(body.get("warehouseId").toString());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

            List<InventoryLedger> saved = new ArrayList<>();
            for (Map<String, Object> item : items) {
                Long materialId = Long.valueOf(item.get("materialId").toString());
                BigDecimal quantity = new BigDecimal(item.get("quantity").toString());
                BigDecimal unitCost = item.get("unitCost") != null
                        ? new BigDecimal(item.get("unitCost").toString())
                        : BigDecimal.ZERO;

                InventoryLedger ledger = inventoryLedgerRepository
                        .findByWarehouseIdAndMaterialId(warehouseId, materialId)
                        .orElse(new InventoryLedger());

                ledger.setWarehouseId(warehouseId);
                ledger.setMaterialId(materialId);
                ledger.setQuantity(quantity);
                ledger.setUnitCost(unitCost);
                ledger.setTotalCost(quantity.multiply(unitCost));
                saved.add(inventoryLedgerRepository.save(ledger));

                InventoryTransaction tx = new InventoryTransaction();
                tx.setWarehouseId(warehouseId);
                tx.setMaterialId(materialId);
                tx.setTransactionType("入库");
                tx.setQuantity(quantity);
                tx.setUnitCost(unitCost);
                tx.setTotalCost(quantity.multiply(unitCost));
                tx.setRelatedOrderNo("INIT");
                inventoryTransactionRepository.save(tx);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
