package com.company.material.service;

import com.company.material.entity.*;
import com.company.material.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReturnOrderService {

    private final ReturnOrderRepository returnOrderRepository;
    private final ReturnOrderItemRepository returnOrderItemRepository;
    private final OutboundOrderRepository outboundOrderRepository;
    private final InventoryLedgerRepository inventoryLedgerRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Transactional
    public ReturnOrder createReturn(ReturnOrder order, List<ReturnOrderItem> items, Long userId) {
        OutboundOrder original = outboundOrderRepository.findById(order.getOriginalOrderId())
                .orElseThrow(() -> new RuntimeException("原出库单不存在"));
        if (!"已出库".equals(original.getStatus())) {
            throw new RuntimeException("原出库单未出库，不能退料");
        }

        order.setReturnNo(generateReturnNo());
        order.setOriginalOrderNo(original.getOrderNo());
        order.setDepartment(original.getDepartment());
        order.setWarehouseId(original.getTargetWarehouseId());
        order.setStatus("待审批");
        order.setCreatedBy(userId);

        ReturnOrder saved = returnOrderRepository.save(order);
        for (ReturnOrderItem item : items) {
            item.setId(null);
            item.setReturnOrderId(saved.getId());
            returnOrderItemRepository.save(item);
        }
        return saved;
    }

    @Transactional
    public ReturnOrder approveReturn(Long returnId, Long approverId, String role) {
        ReturnOrder order = returnOrderRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("退料单不存在"));
        if (!"待审批".equals(order.getStatus())) {
            throw new RuntimeException("当前状态不允许审批");
        }
        order.setStatus("已批准");
        order.setApprovedBy(approverId);
        order.setApprovedAt(LocalDateTime.now());
        return returnOrderRepository.save(order);
    }

    @Transactional
    public ReturnOrder rejectReturn(Long returnId, Long approverId, String reason) {
        ReturnOrder order = returnOrderRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("退料单不存在"));
        if (!"待审批".equals(order.getStatus())) {
            throw new RuntimeException("当前状态不允许驳回");
        }
        order.setStatus("已驳回");
        order.setRejectReason(reason);
        order.setApprovedBy(approverId);
        order.setApprovedAt(LocalDateTime.now());
        return returnOrderRepository.save(order);
    }

    @Transactional
    public ReturnOrder postReturn(Long returnId, Long posterId) {
        ReturnOrder order = returnOrderRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("退料单不存在"));
        if (!"已批准".equals(order.getStatus())) {
            throw new RuntimeException("只有已批准的退料单才能过账");
        }

        List<ReturnOrderItem> items = returnOrderItemRepository.findByReturnOrderId(returnId);
        for (ReturnOrderItem item : items) {
            InventoryLedger ledger = inventoryLedgerRepository
                    .findByWarehouseIdAndMaterialId(order.getWarehouseId(), item.getMaterialId())
                    .orElse(null);

            BigDecimal returnUnitCost = item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO;
            BigDecimal returnTotalCost = returnUnitCost.multiply(item.getQuantity());

            if (ledger == null) {
                ledger = new InventoryLedger();
                ledger.setWarehouseId(order.getWarehouseId());
                ledger.setMaterialId(item.getMaterialId());
                ledger.setQuantity(item.getQuantity());
                ledger.setUnitCost(returnUnitCost);
                ledger.setTotalCost(returnTotalCost);
            } else {
                BigDecimal newQty = ledger.getQuantity().add(item.getQuantity());
                BigDecimal newTotalCost = ledger.getTotalCost().add(returnTotalCost);
                BigDecimal newUnitCost = newQty.compareTo(BigDecimal.ZERO) > 0
                        ? newTotalCost.divide(newQty, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                ledger.setQuantity(newQty);
                ledger.setUnitCost(newUnitCost);
                ledger.setTotalCost(newTotalCost);
            }
            inventoryLedgerRepository.save(ledger);

            InventoryTransaction tx = new InventoryTransaction();
            tx.setWarehouseId(order.getWarehouseId());
            tx.setMaterialId(item.getMaterialId());
            tx.setTransactionType("退料");
            tx.setQuantity(item.getQuantity());
            tx.setUnitCost(returnUnitCost);
            tx.setTotalCost(returnTotalCost);
            tx.setRelatedOrderNo(order.getReturnNo());
            inventoryTransactionRepository.save(tx);
        }

        order.setStatus("已入库");
        order.setPostedBy(posterId);
        order.setPostedAt(LocalDateTime.now());
        return returnOrderRepository.save(order);
    }

    private String generateReturnNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "TL" + date;
        String maxNo = returnOrderRepository.findMaxReturnNoByPrefix(prefix + "%");
        if (maxNo == null) {
            return prefix + "0001";
        }
        String seqStr = maxNo.substring(prefix.length());
        int seq = Integer.parseInt(seqStr) + 1;
        return prefix + String.format("%04d", seq);
    }
}
