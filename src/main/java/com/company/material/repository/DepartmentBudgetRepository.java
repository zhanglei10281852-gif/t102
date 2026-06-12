package com.company.material.repository;

import com.company.material.entity.DepartmentBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DepartmentBudgetRepository extends JpaRepository<DepartmentBudget, Long> {
    Optional<DepartmentBudget> findByDepartmentAndYearMonth(String department, String yearMonth);
    List<DepartmentBudget> findByDepartment(String department);
    List<DepartmentBudget> findByYearMonth(String yearMonth);
}
