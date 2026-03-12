package com.servicehub.repository;

import com.servicehub.model.ServiceRequest;

import java.time.OffsetDateTime;
import java.util.List;
import com.servicehub.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    @Query("""
        SELECT r FROM ServiceRequest r
        WHERE r.status != 'RESOLVED'
        AND r.slaDeadline <= :now
        AND r.isSlaBreached = false
    """)
    List<ServiceRequest> findRequestsPastDeadline(OffsetDateTime now);

    List<ServiceRequest> findAllByRequester(User requester);

    Optional<ServiceRequest> findByIdAndRequester(UUID id, User requester);
}
