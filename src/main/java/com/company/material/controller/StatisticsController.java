package com.company.material.controller;

import com.company.material.entity.Material;
import com.company.material.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final OutboundOrderRepository outboundOrderRepository;
    private final OutboundOrderItemRepository outboundOrderItemRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final MaterialRepository materialRepository;
    private final DepartmentBudgetRepository departmentBudgetRepository;

    @GetMapping("/outbound-monthly")
    public ResponseEntity<?> outboundMonthly(@RequestParam String yearMonth) {
        LocalDate date = LocalDate.parse(yearMonth + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusMonths(1).atStartOfDay();

        BigDecimal totalAmount = outboundOrderRepository.sumPostedAmountByPeriod(start, end);
        List<Object[]> typeBreakdown = outboundOrderRepository.countByTypeAndPeriod(start, end);

        List<Map<String, Object>> types = new ArrayList<>();
        for (Object[] row : typeBreakdown) {
            Map<String, Object> m = new HashMap<>();
            m.put("outboundType", row[0]);
            m.put("count", row[1]);
            types.add(m);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("yearMonth", yearMonth);
        result.put("totalAmount", totalAmount);
        result.put("typeBreakdown", types);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/department-ranking")
    public ResponseEntity<?> departmentRanking(
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) @Deprecated String startMonth,
            @RequestParam(required = false) @Deprecated String endMonth) {
        LocalDateTime start;
        LocalDateTime end;
        if (yearMonth != null && !yearMonth.isBlank()) {
            LocalDate date = LocalDate.parse(yearMonth + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            start = date.atStartOfDay();
            end = date.plusMonths(1).atStartOfDay();
        } else {
            start = LocalDate.now().withDayOfYear(1).atStartOfDay();
            end = LocalDate.now().plusMonths(1).atStartOfDay();
        }

        List<Object[]> ranking = outboundOrderRepository.rankDepartmentAmount(start, end);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : ranking) {
            Map<String, Object> m = new HashMap<>();
            m.put("department", row[0]);
            m.put("totalAmount", row[1]);
            list.add(m);
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/material-ranking")
    public ResponseEntity<?> materialRanking(
            @RequestParam(required = false) String yearMonth,
            @RequestParam(defaultValue = "10") int topN) {
        LocalDateTime start;
        LocalDateTime end;
        if (yearMonth != null && !yearMonth.isBlank()) {
            LocalDate date = LocalDate.parse(yearMonth + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            start = date.atStartOfDay();
            end = date.plusMonths(1).atStartOfDay();
        } else {
            start = LocalDate.now().withDayOfYear(1).atStartOfDay();
            end = LocalDate.now().plusMonths(1).atStartOfDay();
        }

        List<Object[]> ranking = inventoryTransactionRepository.rankMaterialOutboundQuantity(start, end);
        List<Map<String, Object>> list = new ArrayList<>();
        int count = 0;
        for (Object[] row : ranking) {
            if (count >= topN) break;
            Long materialId = (Long) row[0];
            BigDecimal quantity = (BigDecimal) row[1];
            BigDecimal totalCost = (BigDecimal) row[2];

            Map<String, Object> m = new HashMap<>();
            m.put("materialId", materialId);
            m.put("quantity", quantity);
            m.put("totalCost", totalCost);

            materialRepository.findById(materialId).ifPresent(mat -> {
                m.put("materialCode", mat.getMaterialCode());
                m.put("materialName", mat.getName());
                m.put("unit", mat.getUnit());
            });
            list.add(m);
            count++;
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/approval-rate")
    public ResponseEntity<?> approvalRate(@RequestParam(required = false) String yearMonth) {
        LocalDateTime start;
        LocalDateTime end;
        if (yearMonth != null && !yearMonth.isBlank()) {
            LocalDate date = LocalDate.parse(yearMonth + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            start = date.atStartOfDay();
            end = date.plusMonths(1).atStartOfDay();
        } else {
            start = LocalDate.now().withDayOfYear(1).atStartOfDay();
            end = LocalDate.now().plusMonths(1).atStartOfDay();
        }

        long total = outboundOrderRepository.countByCreatedAtBetween(start, end);
        long approved = outboundOrderRepository.countByStatusAndCreatedAtBetween("已批准", start, end)
                + outboundOrderRepository.countByStatusAndCreatedAtBetween("已出库", start, end);
        long rejected = outboundOrderRepository.countByStatusAndCreatedAtBetween("已驳回", start, end);
        long pending = outboundOrderRepository.countByStatusAndCreatedAtBetween("待审批", start, end);

        BigDecimal rate = total > 0
                ? new BigDecimal(approved).divide(new BigDecimal(total), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("approved", approved);
        result.put("rejected", rejected);
        result.put("pending", pending);
        result.put("approvalRate", rate.setScale(2, RoundingMode.HALF_UP) + "%");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/budget-execution")
    public ResponseEntity<?> budgetExecution(@RequestParam String yearMonth) {
        List<com.company.material.entity.DepartmentBudget> budgets = departmentBudgetRepository.findByYearMonth(yearMonth);
        List<Map<String, Object>> list = new ArrayList<>();
        for (com.company.material.entity.DepartmentBudget budget : budgets) {
            LocalDate date = LocalDate.parse(yearMonth + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusMonths(1).atStartOfDay();

            List<com.company.material.entity.OutboundOrder> posted =
                    outboundOrderRepository.findPostedByDeptAndPeriod(budget.getDepartment(), start, end);
            BigDecimal used = posted.stream().map(com.company.material.entity.OutboundOrder::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal ratio = budget.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0
                    ? used.divide(budget.getBudgetAmount(), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Map<String, Object> m = new HashMap<>();
            m.put("department", budget.getDepartment());
            m.put("budgetAmount", budget.getBudgetAmount());
            m.put("usedAmount", used);
            m.put("remainingAmount", budget.getBudgetAmount().subtract(used));
            m.put("usageRatio", ratio);
            m.put("warning", ratio.compareTo(new BigDecimal("0.8")) >= 0);
            m.put("overBudget", ratio.compareTo(BigDecimal.ONE) > 0);
            list.add(m);
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/dept-monthly-comparison")
    public ResponseEntity<?> deptMonthlyComparison(
            @RequestParam String startMonth,
            @RequestParam String endMonth) {
        LocalDate startDate = LocalDate.parse(startMonth + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate endDate = LocalDate.parse(endMonth + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")).plusMonths(1);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atStartOfDay();

        List<Object[]> data = outboundOrderRepository.deptMonthlyAmount(start, end);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : data) {
            Map<String, Object> m = new HashMap<>();
            m.put("department", row[0]);
            m.put("yearMonth", row[1]);
            m.put("totalAmount", row[2]);
            list.add(m);
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/dept-usage-detail")
    public ResponseEntity<?> deptUsageDetail(
            @RequestParam String department,
            @RequestParam String yearMonth) {
        LocalDate date = LocalDate.parse(yearMonth + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusMonths(1).atStartOfDay();

        List<com.company.material.entity.OutboundOrder> posted =
                outboundOrderRepository.findPostedByDeptAndPeriod(department, start, end);

        BigDecimal totalAmount = posted.stream().map(com.company.material.entity.OutboundOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Long> orderIds = posted.stream().map(com.company.material.entity.OutboundOrder::getId).toList();
        List<com.company.material.entity.OutboundOrderItem> allItems = orderIds.isEmpty()
                ? List.of() : outboundOrderItemRepository.findByOrderIdIn(orderIds);

        Map<Long, BigDecimal> materialQuantityMap = new LinkedHashMap<>();
        Map<Long, BigDecimal> materialCostMap = new LinkedHashMap<>();
        Map<Long, String> materialNameMap = new HashMap<>();
        for (com.company.material.entity.OutboundOrderItem item : allItems) {
            materialQuantityMap.merge(item.getMaterialId(), item.getQuantity(), BigDecimal::add);
            materialCostMap.merge(item.getMaterialId(), item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO, BigDecimal::add);
            materialNameMap.putIfAbsent(item.getMaterialId(), item.getMaterialName());
        }

        List<Map<String, Object>> materialDetails = new ArrayList<>();
        materialQuantityMap.entrySet().stream()
                .sorted((a, b) -> materialCostMap.getOrDefault(b.getKey(), BigDecimal.ZERO)
                        .compareTo(materialCostMap.getOrDefault(a.getKey(), BigDecimal.ZERO)))
                .forEach(entry -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("materialId", entry.getKey());
                    m.put("materialName", materialNameMap.get(entry.getKey()));
                    m.put("quantity", entry.getValue());
                    m.put("totalCost", materialCostMap.getOrDefault(entry.getKey(), BigDecimal.ZERO));
                    materialDetails.add(m);
                });

        Map<String, Object> result = new HashMap<>();
        result.put("department", department);
        result.put("yearMonth", yearMonth);
        result.put("totalAmount", totalAmount);
        result.put("orderCount", posted.size());
        result.put("materialDetails", materialDetails);
        return ResponseEntity.ok(result);
    }
}
