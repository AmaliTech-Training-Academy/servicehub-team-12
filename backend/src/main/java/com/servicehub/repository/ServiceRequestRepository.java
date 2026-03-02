package com.servicehub.repository;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByStatus(RequestStatus status);
    List<ServiceRequest> findByCategory(RequestCategory category);
    List<ServiceRequest> findByAssignedToId(Long agentId);
    List<ServiceRequest> findByRequesterId(Long requesterId);
    Long countByStatus(RequestStatus status);
    List<ServiceRequest> findBySlaDeadlineBeforeAndStatusNotIn(LocalDateTime deadline, List<RequestStatus> statuses);
}
