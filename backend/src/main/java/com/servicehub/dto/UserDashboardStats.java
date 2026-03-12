package com.servicehub.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stats returned by {@code GET /api/v1/dashboard/me} for the currently
 * authenticated user's own service-request activity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardStats {

    /** Number of the user's tickets that are currently {@code OPEN}. */
    private long openCount;

    /** Number of the user's tickets that have been {@code RESOLVED}. */
    private long resolvedCount;

    /** Total number of service requests ever submitted by the user. */
    private long totalCount;

    /**
     * The five most-recently created requests, ordered newest-first.
     * Used to populate the "Recent Tickets" table on the user dashboard.
     */
    private List<ServiceRequestResponse> recentTickets;
}

