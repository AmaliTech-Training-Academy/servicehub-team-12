package com.servicehub.repository;

import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    Optional<User> findByEmail(String email);

    /** All users ordered newest-first (no filter). */
    List<User> findAllByOrderByCreatedAtDesc();

    /**
     * Filter by name/email keyword AND/OR role.
     * Pass an empty string for {@code query} to match every row.
     * Pass {@code null} for {@code role} to skip the role filter.
     */
    @Query("""
            SELECT u FROM User u
            WHERE (:query = '' OR
                   LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR
                   LOWER(u.email)    LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:role IS NULL OR u.role = :role)
            ORDER BY u.createdAt DESC
            """)
    List<User> search(@Param("query") String query, @Param("role") Role role);

    List<User> findAllByRoleAndDepartmentIgnoreCaseOrderByCreatedAtAsc(Role role, String department);
}
