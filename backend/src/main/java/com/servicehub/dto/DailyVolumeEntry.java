package com.servicehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Daily volume entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyVolumeEntry {
    private String reportDate;
    private String category;
    private String priority;
    private String status;
    private int    ticketCount;
    private String lastUpdatedAt;
}

