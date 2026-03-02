package com.servicehub.repository;

import com.servicehub.model.SlaPolicy;
import com.servicehub.model.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {
    Optional<SlaPolicy> findByPriority(Priority priority);
}
