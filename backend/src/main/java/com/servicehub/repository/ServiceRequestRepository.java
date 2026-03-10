package com.servicehub.repository;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    List<ServiceRequest> findAllByRequester(User requester);

    Optional<ServiceRequest> findByIdAndRequester(UUID id, User requester);
}
