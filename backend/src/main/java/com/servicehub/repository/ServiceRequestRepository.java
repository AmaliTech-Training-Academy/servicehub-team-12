package com.servicehub.repository;

import com.servicehub.model.ServiceRequest;

import java.time.OffsetDateTime;
import java.util.List;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Count of tickets that are currently breached and have not yet been
     * resolved or closed — drives the "Active Breaches (live)" KPI card.
     */
    @Query("""
        SELECT COUNT(sr)
        FROM   ServiceRequest sr
        WHERE  sr.isSlaBreached = true
        AND    sr.status NOT IN :excluded
        """)
    long countActiveBreaches(@Param("excluded") List<RequestStatus> excluded);

    /**
     * Tickets whose SLA deadline falls within the next two hours and have not
     * yet breached — drives the at-risk warning panel on the admin dashboard.
     * Results are ordered by deadline ascending so the most urgent appear first.
     */
    @Query("""
        SELECT sr
        FROM   ServiceRequest sr
        WHERE  sr.isSlaBreached = false
        AND    sr.status NOT IN :excluded
        AND    sr.slaDeadline IS NOT NULL
        AND    sr.slaDeadline BETWEEN :now AND :threshold
        ORDER  BY sr.slaDeadline ASC
        """)
    List<ServiceRequest> findAtRiskTickets(
        @Param("excluded")  List<RequestStatus> excluded,
        @Param("now")       OffsetDateTime now,
        @Param("threshold") OffsetDateTime threshold
    );
}
