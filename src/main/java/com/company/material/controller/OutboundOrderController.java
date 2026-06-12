package com.company.material.controller;

import com.company.material.entity.OutboundOrder;
import com.company.material.entity.OutboundOrderItem;
import com.company.material.repository.OutboundOrderItemRepository;
import com.company.material.repository.OutboundOrderRepository;
import com.company.material.service.OutboundOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/outbound-orders")
@RequiredArgsConstructor
public class OutboundOrderController {

    private final OutboundOrderService outboundOrderService;
    private final OutboundOrderRepository outboundOrderRepository;
    private final OutboundOrderItemRepository outboundOrderItemRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body,
                                    @RequestAttribute("userId") Long userId,
                                    @RequestAttribute("role") String role) {
        try {
            OutboundOrder order = new OutboundOrder();
            order.setOutboundType((String) body.get("outboundType"));
            order.setDepartment((String) body.get("department"));
            order.setApplicant((String) body.get("applicant"));
            order.setTargetWarehouseId(Long.valueOf(body.get("targetWarehouseId").toString()));
            order.setPurpose((String) body.get("purpose"));
            order.setOutboundDate(java.time.LocalDate.parse((String) body.get("outboundDate")));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemMaps = (List<Map<String, Object>>) body.get("items");
            if (itemMaps == null || itemMaps.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "出库明细不能为空"));
            }

            List<OutboundOrderItem> items = new java.util.ArrayList<>();
            for (Map<String, Object> im : itemMaps) {
                OutboundOrderItem item = new OutboundOrderItem();
                item.setMaterialId(Long.valueOf(im.get("materialId").toString()));
                item.setMaterialCode((String) im.get("materialCode"));
                item.setMaterialName((String) im.get("materialName"));
                item.setQuantity(new java.math.BigDecimal(im.get("quantity").toString()));
                item.setUnit((String) im.get("unit"));
                if (im.get("referencePrice") != null) {
                    item.setReferencePrice(new java.math.BigDecimal(im.get("referencePrice").toString()));
                }
                items.add(item);
            }

            OutboundOrder saved = outboundOrderService.createOrder(order, items, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("order", saved);
            result.put("items", outboundOrderItemRepository.findByOrderId(saved.getId()));
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String outboundType) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OutboundOrder> result;
        if (status != null && !status.isBlank()) {
            result = outboundOrderRepository.findByStatus(status, pr);
        } else if (department != null && !department.isBlank()) {
            result = outboundOrderRepository.findByDepartment(department, pr);
        } else if (outboundType != null && !outboundType.isBlank()) {
            result = outboundOrderRepository.findByOutboundType(outboundType, pr);
        } else {
            result = outboundOrderRepository.findAll(pr);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return outboundOrderRepository.findById(id).map(order -> {
            Map<String, Object> result = new HashMap<>();
            result.put("order", order);
            result.put("items", outboundOrderItemRepository.findByOrderId(id));
            return ResponseEntity.ok((Object) result);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id,
                                     @RequestAttribute("userId") Long userId,
                                     @RequestAttribute("role") String role) {
        try {
            OutboundOrder order = outboundOrderService.approveOrder(id, userId, role);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id,
                                    @RequestAttribute("userId") Long userId,
                                    @RequestBody Map<String, String> body) {
        try {
            String reason = body.get("reason");
            OutboundOrder order = outboundOrderService.rejectOrder(id, userId, reason);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<?> post(@PathVariable Long id,
                                  @RequestAttribute("userId") Long userId) {
        try {
            OutboundOrder order = outboundOrderService.postOrder(id, userId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/budget-warning")
    public ResponseEntity<?> budgetWarning(@RequestParam String department,
                                           @RequestParam String yearMonth) {
        return ResponseEntity.ok(outboundOrderService.checkBudgetWarning(department, yearMonth));
    }
}
