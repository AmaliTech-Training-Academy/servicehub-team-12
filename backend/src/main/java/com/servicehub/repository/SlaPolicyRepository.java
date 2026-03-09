package com.servicehub.repository;

import com.servicehub.model.SlaPolicy;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.RequestPriority;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, UUID> {
    Optional<SlaPolicy> findByCategoryAndPriority(RequestCategory category, RequestPriority priority);
}
