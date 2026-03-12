package com.servicehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Projection used by the admin dashboard "Agents" tab leaderboard.
 * Fields match the Thymeleaf bindings in {@code dashboard/admin.html}
 * and the inline JavaScript {@code leaderboard} variable.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentLeaderboardEntry {

    /** Display name of the agent. */
    private String agentName;

    /** Total tickets assigned during the week. */
    private int ticketsAssigned;

    /** Total tickets resolved during the week. */
    private int ticketsResolved;

    /** Average resolution time in hours. */
    private double avgResolutionHours;

    /** SLA compliance percentage (0-100). */
    private double slaComplianceRatePct;

    /** ISO date string of the week start (yyyy-MM-dd). */
    private String weekStart;
}

