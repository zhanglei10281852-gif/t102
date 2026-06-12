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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OutboundOrderService {

    private final OutboundOrderRepository outboundOrderRepository;
    private final OutboundOrderItemRepository outboundOrderItemRepository;
    private final InventoryLedgerRepository inventoryLedgerRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final MaterialRepository materialRepository;
    private final DepartmentBudgetRepository departmentBudgetRepository;

    @Transactional
    public OutboundOrder createOrder(OutboundOrder order, List<OutboundOrderItem> items, Long userId) {
        order.setOrderNo(generateOrderNo());
        order.setStatus("待审批");
        order.setCreatedBy(userId);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OutboundOrderItem item : items) {
            item.setId(null);
            item.setOrderId(null);
            if (item.getReferencePrice() != null && item.getQuantity() != null) {
                item.setAmount(item.getReferencePrice().multiply(item.getQuantity()));
            } else {
                item.setAmount(BigDecimal.ZERO);
            }
            totalAmount = totalAmount.add(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
        }
        order.setTotalAmount(totalAmount);

        OutboundOrder saved = outboundOrderRepository.save(order);
        for (OutboundOrderItem item : items) {
            item.setOrderId(saved.getId());
            outboundOrderItemRepository.save(item);
        }
        return saved;
    }

    @Transactional
    public OutboundOrder approveOrder(Long orderId, Long approverId, String role) {
        OutboundOrder order = outboundOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("出库单不存在"));
        if (!"待审批".equals(order.getStatus())) {
            throw new RuntimeException("当前状态不允许审批");
        }

        BigDecimal threshold = new BigDecimal("2000");
        if (order.getTotalAmount().compareTo(threshold) >= 0 && !"管理员".equals(role)) {
            throw new RuntimeException("出库总金额≥2000元，需管理员审批");
        }

        order.setStatus("已批准");
        order.setApprovedBy(approverId);
        order.setApprovedAt(LocalDateTime.now());
        return outboundOrderRepository.save(order);
    }

    @Transactional
    public OutboundOrder rejectOrder(Long orderId, Long approverId, String reason) {
        OutboundOrder order = outboundOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("出库单不存在"));
        if (!"待审批".equals(order.getStatus())) {
            throw new RuntimeException("当前状态不允许驳回");
        }
        order.setStatus("已驳回");
        order.setRejectReason(reason);
        order.setApprovedBy(approverId);
        order.setApprovedAt(LocalDateTime.now());
        return outboundOrderRepository.save(order);
    }

    @Transactional
    public OutboundOrder postOrder(Long orderId, Long posterId) {
        OutboundOrder order = outboundOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("出库单不存在"));
        if (!"已批准".equals(order.getStatus())) {
            throw new RuntimeException("只有已批准的出库单才能过账");
        }

        List<OutboundOrderItem> items = outboundOrderItemRepository.findByOrderId(orderId);
        List<String> shortages = new ArrayList<>();

        for (OutboundOrderItem item : items) {
            InventoryLedger ledger = inventoryLedgerRepository
                    .findByWarehouseIdAndMaterialId(order.getTargetWarehouseId(), item.getMaterialId())
                    .orElse(null);
            if (ledger == null || ledger.getQuantity().compareTo(item.getQuantity()) < 0) {
                BigDecimal onHand = ledger != null ? ledger.getQuantity() : BigDecimal.ZERO;
                BigDecimal shortage = item.getQuantity().subtract(onHand);
                shortages.add(item.getMaterialName() + "缺" + shortage.setScale(2, RoundingMode.HALF_UP) + item.getUnit());
            }
        }

        if (!shortages.isEmpty()) {
            throw new RuntimeException("库存不足，无法过账: " + String.join("; ", shortages));
        }

        for (OutboundOrderItem item : items) {
            InventoryLedger ledger = inventoryLedgerRepository
                    .findByWarehouseIdAndMaterialId(order.getTargetWarehouseId(), item.getMaterialId())
                    .orElseThrow();

            BigDecimal newQty = ledger.getQuantity().subtract(item.getQuantity());
            BigDecimal deductCost = ledger.getUnitCost().multiply(item.getQuantity());
            BigDecimal newTotalCost = ledger.getTotalCost().subtract(deductCost);
            BigDecimal newUnitCost = newQty.compareTo(BigDecimal.ZERO) > 0
                    ? newTotalCost.divide(newQty, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            ledger.setQuantity(newQty);
            ledger.setUnitCost(newUnitCost);
            ledger.setTotalCost(newQty.compareTo(BigDecimal.ZERO) > 0 ? newTotalCost : BigDecimal.ZERO);
            inventoryLedgerRepository.save(ledger);

            InventoryTransaction tx = new InventoryTransaction();
            tx.setWarehouseId(order.getTargetWarehouseId());
            tx.setMaterialId(item.getMaterialId());
            tx.setTransactionType("出库");
            tx.setQuantity(item.getQuantity());
            tx.setUnitCost(ledger.getUnitCost());
            tx.setTotalCost(deductCost);
            tx.setRelatedOrderNo(order.getOrderNo());
            inventoryTransactionRepository.save(tx);
        }

        order.setStatus("已出库");
        order.setPostedBy(posterId);
        order.setPostedAt(LocalDateTime.now());
        return outboundOrderRepository.save(order);
    }

    public Map<String, Object> checkBudgetWarning(String department, String yearMonth) {
        Map<String, Object> result = new HashMap<>();
        Optional<DepartmentBudget> budgetOpt = departmentBudgetRepository.findByDepartmentAndYearMonth(department, yearMonth);
        if (budgetOpt.isEmpty()) {
            result.put("hasBudget", false);
            return result;
        }

        DepartmentBudget budget = budgetOpt.get();
        LocalDateTime start = LocalDate.parse(yearMonth + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);

        List<OutboundOrder> posted = outboundOrderRepository.findPostedByDeptAndPeriod(department, start, end);
        BigDecimal used = posted.stream().map(OutboundOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ratio = budget.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0
                ? used.divide(budget.getBudgetAmount(), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        result.put("hasBudget", true);
        result.put("budgetAmount", budget.getBudgetAmount());
        result.put("usedAmount", used);
        result.put("remainingAmount", budget.getBudgetAmount().subtract(used));
        result.put("usageRatio", ratio);
        result.put("warning", ratio.compareTo(new BigDecimal("0.8")) >= 0);
        result.put("overBudget", ratio.compareTo(BigDecimal.ONE) > 0);
        return result;
    }

    private String generateOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "CK" + date;
        String maxNo = outboundOrderRepository.findMaxOrderNoByPrefix(prefix + "%");
        if (maxNo == null) {
            return prefix + "0001";
        }
        String seqStr = maxNo.substring(prefix.length());
        int seq = Integer.parseInt(seqStr) + 1;
        return prefix + String.format("%04d", seq);
    }
}
