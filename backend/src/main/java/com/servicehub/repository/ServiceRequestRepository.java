package com.servicehub.repository;

import com.servicehub.model.ServiceRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    List<ServiceRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);
}
