package com.servicehub.dto;

import com.servicehub.model.enums.RequestPriority;
import com.servicehub.model.enums.RequestStatus;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Builder
public class ServiceRequestPageQuery {

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 10;

    private String q;
    private RequestStatus status;
    private RequestPriority priority;
    private UUID requesterId;
    private UUID assignedToId;

    @Builder.Default
    private boolean breached = false;

    public Pageable toPageable() {
        return PageRequest.of(
                Math.max(page - 1, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    public String normalizedQuery() {
        if (q == null) {
            return null;
        }

        String trimmed = q.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public ServiceRequestPageQuery forRequester(UUID userId) {
        return ServiceRequestPageQuery.builder()
                .page(page)
                .size(size)
                .q(q)
                .status(status)
                .priority(priority)
                .requesterId(userId)
                .assignedToId(assignedToId)
                .breached(breached)
                .build();
    }

    public ServiceRequestPageQuery forAssignee(UUID userId) {
        return ServiceRequestPageQuery.builder()
                .page(page)
                .size(size)
                .q(q)
                .status(status)
                .priority(priority)
                .requesterId(requesterId)
                .assignedToId(userId)
                .breached(breached)
                .build();
    }
}
