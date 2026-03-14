package com.servicehub.repository.specification;

import com.servicehub.dto.UserPageQuery;
import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import org.springframework.data.jpa.domain.Specification;
/**
 * Builds JPA specifications for filtering users.
 */

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> fromQuery(UserPageQuery query) {
        return Specification.allOf(
                matchesSearch(query.normalizedQuery()),
                hasRole(query.getRole())
        );
    }

    private static Specification<User> matchesSearch(String search) {
        return (root, query, cb) -> {
            if (search == null) {
                return cb.conjunction();
            }

            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
            );
        };
    }

    private static Specification<User> hasRole(Role role) {
        return (root, query, cb) -> role == null
                ? cb.conjunction()
                : cb.equal(root.get("role"), role);
    }
}
