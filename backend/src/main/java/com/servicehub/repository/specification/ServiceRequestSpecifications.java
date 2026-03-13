package com.servicehub.repository.specification;

import com.servicehub.dto.ServiceRequestPageQuery;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.RequestStatus;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ServiceRequestSpecifications {

    private static final List<RequestStatus> DONE_STATUSES = List.of(RequestStatus.RESOLVED, RequestStatus.CLOSED);

    private ServiceRequestSpecifications() {
    }

    public static Specification<ServiceRequest> fromQuery(ServiceRequestPageQuery query) {
        return Specification.allOf(
                hasRequester(query.getRequesterId()),
                hasAssignedTo(query.getAssignedToId()),
                matchesSearch(query.normalizedQuery()),
                hasStatus(query.getStatus()),
                hasPriority(query.getPriority()),
                breachedOnly(query.isBreached())
        );
    }

    private static Specification<ServiceRequest> hasRequester(java.util.UUID requesterId) {
        return (root, query, cb) -> requesterId == null
                ? cb.conjunction()
                : cb.equal(root.join("requester", JoinType.LEFT).get("id"), requesterId);
    }

    private static Specification<ServiceRequest> hasAssignedTo(java.util.UUID assignedToId) {
        return (root, query, cb) -> assignedToId == null
                ? cb.conjunction()
                : cb.equal(root.join("assignedTo", JoinType.LEFT).get("id"), assignedToId);
    }

    private static Specification<ServiceRequest> matchesSearch(String search) {
        return (root, query, cb) -> {
            if (search == null) {
                return cb.conjunction();
            }

            String pattern = "%" + search.toLowerCase() + "%";
            var requester = root.join("requester", JoinType.LEFT);
            var assignedTo = root.join("assignedTo", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(requester.get("fullName")), pattern),
                    cb.like(cb.lower(requester.get("email")), pattern),
                    cb.like(cb.lower(assignedTo.get("fullName")), pattern)
            );
        };
    }

    private static Specification<ServiceRequest> hasStatus(RequestStatus status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    private static Specification<ServiceRequest> hasPriority(com.servicehub.model.enums.RequestPriority priority) {
        return (root, query, cb) -> priority == null
                ? cb.conjunction()
                : cb.equal(root.get("priority"), priority);
    }

    private static Specification<ServiceRequest> breachedOnly(boolean breached) {
        return (root, query, cb) -> !breached
                ? cb.conjunction()
                : cb.and(
                        cb.isTrue(root.get("isSlaBreached")),
                        cb.not(root.get("status").in(DONE_STATUSES))
                );
    }
}
