
package com.servicehub.service;

import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.dto.ServiceRequestUpsertRequest;
import java.util.List;
import java.util.UUID;

public interface ServiceRequestService {
    ServiceRequestResponse create(ServiceRequestUpsertRequest request);

    List<ServiceRequestResponse> findAll();

    /** Returns only tickets where the requester matches userId. */
    List<ServiceRequestResponse> findAllByRequesterId(UUID userId);

    ServiceRequestResponse findById(UUID id);

    /** Returns the ticket only if it belongs to userId; throws 403 otherwise. */
    ServiceRequestResponse findByIdForUser(UUID id, UUID userId);

    ServiceRequestResponse update(UUID id, ServiceRequestUpsertRequest request);

    void delete(UUID id);
}

