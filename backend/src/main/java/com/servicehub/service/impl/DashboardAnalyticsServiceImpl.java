package com.servicehub.service.impl;

import com.servicehub.dto.AgentLeaderboardEntry;
import com.servicehub.dto.DailyVolumeEntry;
import com.servicehub.dto.DeptWorkloadEntry;
import com.servicehub.dto.SlaMetricEntry;
import com.servicehub.repository.AnalyticsDashboardRepository;
import com.servicehub.service.DashboardAnalyticsService;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link DashboardAnalyticsService}.
 *
 * <p>All data is read from the four {@code analytics_*} tables written by the
 * Airflow ETL pipeline. This class must <em>not</em> recompute any metrics —
 * it delegates all SQL work to {@link AnalyticsDashboardRepository} and returns
 * the results as-is to the caller.
 */
@Service
@RequiredArgsConstructor
public class DashboardAnalyticsServiceImpl implements DashboardAnalyticsService {

    private final AnalyticsDashboardRepository analyticsRepository;

    /** {@inheritDoc} */
    @Override
    public List<SlaMetricEntry> getSlaMetrics() {
        return analyticsRepository.findAllSlaMetrics();
    }

    /** {@inheritDoc} */
    @Override
    public List<DailyVolumeEntry> getDailyVolume() {
        return analyticsRepository.findAllDailyVolume();
    }

    /** {@inheritDoc} */
    @Override
    public List<AgentLeaderboardEntry> getLeaderboard() {
        return analyticsRepository.findCurrentWeekLeaderboard();
    }

    /** {@inheritDoc} */
    @Override
    public List<AgentLeaderboardEntry> getAgentPerfHistory() {
        return analyticsRepository.findRecentAgentPerfHistory();
    }

    /** {@inheritDoc} */
    @Override
    public List<DeptWorkloadEntry> getDeptWorkload() {
        return analyticsRepository.findCurrentWeekDeptWorkload();
    }

    /** {@inheritDoc} */
    @Override
    public List<DeptWorkloadEntry> getDeptPerfHistory() {
        return analyticsRepository.findRecentDeptPerfHistory();
    }

    /** {@inheritDoc} */
    @Override
    public OffsetDateTime getLastEtlRunTime() {
        return analyticsRepository.findLastEtlRunTime();
    }
}

