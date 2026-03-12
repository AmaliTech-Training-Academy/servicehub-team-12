package com.servicehub.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Headline KPI numbers returned by {@code GET /api/v1/dashboard/admin/kpis}.
 *
 * <ul>
 *   <li>{@code totalTickets}, {@code totalBreached} and {@code overallCompliance}
 *       are aggregated from the Airflow ETL analytics tables.</li>
 *   <li>{@code activeBreaches} and {@code atRiskCount} are <em>live</em> values
 *       queried directly from {@code service_requests} and will differ from the
 *       ETL-sourced numbers between pipeline runs.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminKpiResponse {

    /** Total tickets across all SLA metric rows (ETL-sourced). */
    private int totalTickets;

    /** Total breached tickets across all SLA metric rows (ETL-sourced). */
    private int totalBreached;

    /**
     * Ticket-weighted SLA compliance rate across all categories and priorities
     * (ETL-sourced), rounded to one decimal place.
     */
    private double overallCompliance;

    /** Live count of tickets that are currently SLA-breached and still open (repo-sourced). */
    private long activeBreaches;

    /** Live count of tickets whose SLA deadline falls within the next two hours (repo-sourced). */
    private int atRiskCount;

    /**
     * Timestamp of the most-recent Airflow ETL pipeline run, or {@code null}
     * if no run has occurred yet.
     */
    private OffsetDateTime lastEtlRun;
}

