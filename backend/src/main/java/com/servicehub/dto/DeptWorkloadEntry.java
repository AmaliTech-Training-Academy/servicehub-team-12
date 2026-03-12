package com.servicehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Projection used by the admin dashboard "Departments" tab.
 * Fields match the Thymeleaf bindings in {@code dashboard/admin.html}
 * and the inline JavaScript {@code deptWorkload} variable.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeptWorkloadEntry {

    /** Display name of the department. */
    private String departmentName;

    /** Currently open (unresolved) tickets this week. */
    private int openTickets;

    /** Tickets resolved this week. */
    private int resolvedTickets;

    /** Tickets that breached SLA this week. */
    private int breachedTickets;

    /** Average resolution time in hours. */
    private double avgResolutionHours;

    /** ISO date string of the week start (yyyy-MM-dd). */
    private String weekStart;
}

