package com.servicehub.dto;

import com.servicehub.model.enums.Role;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
/**
 * Data transfer object for user pagination and filtering.
 */

@Getter
@Builder
public class UserPageQuery {

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 10;

    private String q;
    private Role role;

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
}
