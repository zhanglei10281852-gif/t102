package com.company.material.controller;

import com.company.material.entity.ReturnOrder;
import com.company.material.entity.ReturnOrderItem;
import com.company.material.repository.ReturnOrderItemRepository;
import com.company.material.repository.ReturnOrderRepository;
import com.company.material.service.ReturnOrderService;
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
@RequestMapping("/api/return-orders")
@RequiredArgsConstructor
public class ReturnOrderController {

    private final ReturnOrderService returnOrderService;
    private final ReturnOrderRepository returnOrderRepository;
    private final ReturnOrderItemRepository returnOrderItemRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body,
                                    @RequestAttribute("userId") Long userId) {
        try {
            ReturnOrder order = new ReturnOrder();
            order.setOriginalOrderId(Long.valueOf(body.get("originalOrderId").toString()));
            order.setApplicant((String) body.get("applicant"));
            order.setReturnDate(java.time.LocalDate.parse((String) body.get("returnDate")));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemMaps = (List<Map<String, Object>>) body.get("items");
            if (itemMaps == null || itemMaps.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "退料明细不能为空"));
            }

            List<ReturnOrderItem> items = new java.util.ArrayList<>();
            for (Map<String, Object> im : itemMaps) {
                ReturnOrderItem item = new ReturnOrderItem();
                item.setMaterialId(Long.valueOf(im.get("materialId").toString()));
                item.setMaterialCode((String) im.get("materialCode"));
                item.setMaterialName((String) im.get("materialName"));
                item.setQuantity(new java.math.BigDecimal(im.get("quantity").toString()));
                item.setUnit((String) im.get("unit"));
                if (im.get("unitCost") != null) {
                    item.setUnitCost(new java.math.BigDecimal(im.get("unitCost").toString()));
                }
                items.add(item);
            }

            ReturnOrder saved = returnOrderService.createReturn(order, items, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("order", saved);
            result.put("items", returnOrderItemRepository.findByReturnOrderId(saved.getId()));
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
            @RequestParam(required = false) String department) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReturnOrder> result;
        if (status != null && !status.isBlank()) {
            result = returnOrderRepository.findByStatus(status, pr);
        } else if (department != null && !department.isBlank()) {
            result = returnOrderRepository.findByDepartment(department, pr);
        } else {
            result = returnOrderRepository.findAll(pr);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return returnOrderRepository.findById(id).map(order -> {
            Map<String, Object> result = new HashMap<>();
            result.put("order", order);
            result.put("items", returnOrderItemRepository.findByReturnOrderId(id));
            return ResponseEntity.ok((Object) result);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-outbound/{outboundId}")
    public ResponseEntity<?> byOutbound(@PathVariable Long outboundId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(returnOrderRepository.findByOriginalOrderId(outboundId, pr));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id,
                                     @RequestAttribute("userId") Long userId,
                                     @RequestAttribute("role") String role) {
        try {
            ReturnOrder order = returnOrderService.approveReturn(id, userId, role);
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
            ReturnOrder order = returnOrderService.rejectReturn(id, userId, reason);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<?> post(@PathVariable Long id,
                                  @RequestAttribute("userId") Long userId) {
        try {
            ReturnOrder order = returnOrderService.postReturn(id, userId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
