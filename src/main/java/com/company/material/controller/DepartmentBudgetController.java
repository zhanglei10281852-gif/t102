package com.company.material.controller;

import com.company.material.entity.DepartmentBudget;
import com.company.material.repository.DepartmentBudgetRepository;
import com.company.material.service.OutboundOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/department-budgets")
@RequiredArgsConstructor
public class DepartmentBudgetController {

    private final DepartmentBudgetRepository departmentBudgetRepository;
    private final OutboundOrderService outboundOrderService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody DepartmentBudget budget) {
        if (budget.getDepartment() == null || budget.getYearMonth() == null || budget.getBudgetAmount() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "部门、月份、预算金额为必填"));
        }
        if (departmentBudgetRepository.findByDepartmentAndYearMonth(budget.getDepartment(), budget.getYearMonth()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "该部门本月预算已存在"));
        }
        budget.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentBudgetRepository.save(budget));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String yearMonth) {
        if (department != null && yearMonth != null) {
            return departmentBudgetRepository.findByDepartmentAndYearMonth(department, yearMonth)
                    .map(b -> ResponseEntity.ok((Object) b))
                    .orElse(ResponseEntity.notFound().build());
        } else if (department != null) {
            return ResponseEntity.ok(departmentBudgetRepository.findByDepartment(department));
        } else if (yearMonth != null) {
            return ResponseEntity.ok(departmentBudgetRepository.findByYearMonth(yearMonth));
        }
        return ResponseEntity.ok(departmentBudgetRepository.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody DepartmentBudget body) {
        return departmentBudgetRepository.findById(id).map(b -> {
            if (body.getBudgetAmount() != null) b.setBudgetAmount(body.getBudgetAmount());
            if (body.getYearMonth() != null) b.setYearMonth(body.getYearMonth());
            return ResponseEntity.ok((Object) departmentBudgetRepository.save(b));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/execution")
    public ResponseEntity<?> execution(@RequestParam String department, @RequestParam String yearMonth) {
        return ResponseEntity.ok(outboundOrderService.checkBudgetWarning(department, yearMonth));
    }
}
