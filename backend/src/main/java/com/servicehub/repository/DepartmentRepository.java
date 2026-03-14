package com.servicehub.repository;

import com.servicehub.model.Department;
import com.servicehub.model.enums.RequestCategory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Provides persistence operations for departments.
 */

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Optional<Department> findByCategory(RequestCategory category);

    Optional<Department> findByNameIgnoreCase(String name);
}
