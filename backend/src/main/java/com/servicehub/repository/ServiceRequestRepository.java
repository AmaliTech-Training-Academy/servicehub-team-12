package com.servicehub.repository;

import com.servicehub.model.ServiceRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {
}
