
package com.servicehub.service;

import com.servicehub.dto.ServiceRequestPageQuery;
import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.dto.ServiceRequestUpsertRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

public interface ServiceRequestService {
    ServiceRequestResponse create(ServiceRequestUpsertRequest request);

    List<ServiceRequestResponse> findAll();

    Page<ServiceRequestResponse> findPage(ServiceRequestPageQuery query);

    /** Returns only tickets where the requester matches userId. */
    List<ServiceRequestResponse> findAllByRequesterId(UUID userId);

    Page<ServiceRequestResponse> findAllByRequesterId(UUID userId, ServiceRequestPageQuery query);

    Page<ServiceRequestResponse> findAllByAssignedToId(UUID userId, ServiceRequestPageQuery query);

    ServiceRequestResponse findById(UUID id);

    /** Returns the ticket only if it belongs to userId; throws 403 otherwise. */
    ServiceRequestResponse findByIdForUser(UUID id, UUID userId);

    ServiceRequestResponse update(UUID id, ServiceRequestUpsertRequest request);

    void autoAssign(UUID id);

    void delete(UUID id);
}
