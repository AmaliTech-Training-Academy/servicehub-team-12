package com.servicehub.dto;

import lombok.*;
import java.util.Map;
/**
 * Data transfer object for dashboard statistics.
 */

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardStatsResponse {
    private Long totalRequests;
    private Long openRequests;
    private Long resolvedRequests;
    private Double avgResolutionHours;
    private Double slaComplianceRate;
    private Map<String, Long> requestsByCategory;
    private Map<String, Long> requestsByPriority;
}
