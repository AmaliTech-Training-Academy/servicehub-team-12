package com.servicehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaMetricEntry {
    private String category;
    private String priority;
    private int    totalTickets;
    private int    resolvedTickets;
    private int    breachedTickets;
    private double complianceRatePct;
    private double avgResolutionHours;
    private double avgResponseHours;
    private String lastUpdatedAt;
}

