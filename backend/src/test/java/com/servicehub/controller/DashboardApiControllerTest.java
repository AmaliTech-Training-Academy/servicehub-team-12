package com.servicehub.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.servicehub.controller.api.DashboardApiController;
import com.servicehub.dto.AgentLeaderboardEntry;
import com.servicehub.dto.DailyVolumeEntry;
import com.servicehub.dto.DeptWorkloadEntry;
import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.dto.SlaMetricEntry;
import com.servicehub.exception.GlobalExceptionHandler;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.service.DashboardAnalyticsService;
import com.servicehub.service.ServiceRequestService;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DashboardApiController")
class DashboardApiControllerTest {

    private MockMvc mockMvc;

    @Mock private DashboardAnalyticsService  analyticsService;
    @Mock private ServiceRequestService      serviceRequestService;
    @Mock private ServiceRequestRepository   serviceRequestRepository;

    @InjectMocks
    private DashboardApiController dashboardApiController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(dashboardApiController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User buildUser(UUID id, Role role) {
        return User.builder()
                .id(id)
                .email("user@example.com")
                .fullName("Test User")
                .role(role)
                .isActive(true)
                .build();
    }

    private void authenticateAs(User user) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private ServiceRequestResponse requestResponse(UUID id, RequestStatus status) {
        return ServiceRequestResponse.builder()
                .id(id)
                .title("Test ticket")
                .status(status)
                .build();
    }

    private SlaMetricEntry slaEntry(String category, String priority,
                                    int total, int breached, double compliance) {
        return SlaMetricEntry.builder()
                .category(category)
                .priority(priority)
                .totalTickets(total)
                .resolvedTickets(total - breached)
                .breachedTickets(breached)
                .complianceRatePct(compliance)
                .avgResolutionHours(5.0)
                .avgResponseHours(1.0)
                .lastUpdatedAt("2026-03-11T22:30:00Z")
                .build();
    }

    private DailyVolumeEntry volumeEntry(String date, String category, int count) {
        return DailyVolumeEntry.builder()
                .reportDate(date)
                .category(category)
                .priority("high")
                .status("open")
                .ticketCount(count)
                .lastUpdatedAt("2026-03-11T23:55:12Z")
                .build();
    }

    private AgentLeaderboardEntry agentEntry(String name, double compliance) {
        return AgentLeaderboardEntry.builder()
                .agentName(name)
                .ticketsAssigned(50)
                .ticketsResolved(45)
                .avgResolutionHours(5.0)
                .slaComplianceRatePct(compliance)
                .weekStart("2026-03-02")
                .build();
    }

    private DeptWorkloadEntry deptEntry(String dept, int open, int resolved) {
        return DeptWorkloadEntry.builder()
                .departmentName(dept)
                .openTickets(open)
                .resolvedTickets(resolved)
                .breachedTickets(2)
                .avgResolutionHours(8.0)
                .weekStart("2026-03-02")
                .build();
    }

    // ── GET /api/v1/dashboard/me ──────────────────────────────────────────────

    @Nested
    @DisplayName("GET /me — user dashboard stats")
    class MyStats {

        @Test
        @DisplayName("returns 200 with correct open, resolved and total counts")
        void returnsCorrectCounts() throws Exception {
            UUID userId = UUID.randomUUID();
            User user   = buildUser(userId, Role.USER);
            authenticateAs(user);

            List<ServiceRequestResponse> requests = List.of(
                    requestResponse(UUID.randomUUID(), RequestStatus.OPEN),
                    requestResponse(UUID.randomUUID(), RequestStatus.OPEN),
                    requestResponse(UUID.randomUUID(), RequestStatus.RESOLVED),
                    requestResponse(UUID.randomUUID(), RequestStatus.CLOSED)
            );
            when(serviceRequestService.findAllByRequesterId(userId)).thenReturn(requests);

            mockMvc.perform(get("/api/v1/dashboard/me")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.openCount",     is(2)))
                    .andExpect(jsonPath("$.resolvedCount", is(1)))
                    .andExpect(jsonPath("$.totalCount",    is(4)));

            verify(serviceRequestService).findAllByRequesterId(userId);
        }

        @Test
        @DisplayName("returns empty stats when user has no tickets")
        void returnsZeroCountsWhenNoTickets() throws Exception {
            UUID userId = UUID.randomUUID();
            User user   = buildUser(userId, Role.USER);
            authenticateAs(user);

            when(serviceRequestService.findAllByRequesterId(userId))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/dashboard/me")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.openCount",     is(0)))
                    .andExpect(jsonPath("$.resolvedCount", is(0)))
                    .andExpect(jsonPath("$.totalCount",    is(0)))
                    .andExpect(jsonPath("$.recentTickets", hasSize(0)));
        }

        @Test
        @DisplayName("limits recent tickets to five even when user has more")
        void limitsRecentTicketsToFive() throws Exception {
            UUID userId = UUID.randomUUID();
            User user   = buildUser(userId, Role.USER);
            authenticateAs(user);

            // 8 tickets — only 5 should appear in recentTickets
            List<ServiceRequestResponse> requests = List.of(
                    requestResponse(UUID.randomUUID(), RequestStatus.OPEN),
                    requestResponse(UUID.randomUUID(), RequestStatus.OPEN),
                    requestResponse(UUID.randomUUID(), RequestStatus.OPEN),
                    requestResponse(UUID.randomUUID(), RequestStatus.OPEN),
                    requestResponse(UUID.randomUUID(), RequestStatus.OPEN),
                    requestResponse(UUID.randomUUID(), RequestStatus.RESOLVED),
                    requestResponse(UUID.randomUUID(), RequestStatus.RESOLVED),
                    requestResponse(UUID.randomUUID(), RequestStatus.RESOLVED)
            );
            when(serviceRequestService.findAllByRequesterId(userId)).thenReturn(requests);

            mockMvc.perform(get("/api/v1/dashboard/me")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount",    is(8)))
                    .andExpect(jsonPath("$.recentTickets", hasSize(5)));
        }

        @Test
        @DisplayName("works for AGENT role (all roles share the same endpoint)")
        void worksForAgentRole() throws Exception {
            UUID userId = UUID.randomUUID();
            User agent  = buildUser(userId, Role.AGENT);
            authenticateAs(agent);

            when(serviceRequestService.findAllByRequesterId(userId))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/dashboard/me")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(serviceRequestService).findAllByRequesterId(userId);
        }
    }

    // ── GET /api/v1/dashboard/admin/kpis ─────────────────────────────────────

    @Nested
    @DisplayName("GET /admin/kpis — admin KPI summary")
    class AdminKpis {

        @Test
        @DisplayName("returns 200 with aggregated totals and weighted compliance")
        void returnsAggregatedMetrics() throws Exception {
            // 120 total, 18 breached, 83.64 % compliance
            // 260 total, 30 breached, 88.00 % compliance
            // weighted = (83.64*120 + 88.0*260) / (120+260) = (10036.8 + 22880) / 380 = 86.62...
            List<SlaMetricEntry> sla = List.of(
                    slaEntry("IT_SUPPORT", "CRITICAL", 120, 18, 83.64),
                    slaEntry("IT_SUPPORT", "HIGH",     260, 30, 88.00)
            );
            when(analyticsService.getSlaMetrics()).thenReturn(sla);
            when(analyticsService.getLastEtlRunTime())
                    .thenReturn(OffsetDateTime.parse("2026-03-11T22:30:00Z"));
            when(serviceRequestRepository.countActiveBreaches(anyList())).thenReturn(5L);
            when(serviceRequestRepository.findAtRiskTickets(anyList(), any(), any()))
                    .thenReturn(List.of(new ServiceRequest(), new ServiceRequest(), new ServiceRequest()));

            mockMvc.perform(get("/api/v1/dashboard/admin/kpis")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTickets",   is(380)))
                    .andExpect(jsonPath("$.totalBreached",  is(48)))
                    .andExpect(jsonPath("$.activeBreaches", is(5)))
                    .andExpect(jsonPath("$.atRiskCount",    is(3)));

            verify(analyticsService).getSlaMetrics();
            verify(serviceRequestRepository).countActiveBreaches(anyList());
        }

        @Test
        @DisplayName("returns overallCompliance of 0.0 when no ETL data exists")
        void returnsZeroComplianceWhenNoData() throws Exception {
            when(analyticsService.getSlaMetrics()).thenReturn(Collections.emptyList());
            when(analyticsService.getLastEtlRunTime()).thenReturn(null);
            when(serviceRequestRepository.countActiveBreaches(anyList())).thenReturn(0L);
            when(serviceRequestRepository.findAtRiskTickets(anyList(), any(), any()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/dashboard/admin/kpis")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTickets",     is(0)))
                    .andExpect(jsonPath("$.totalBreached",    is(0)))
                    .andExpect(jsonPath("$.overallCompliance",is(0.0)))
                    .andExpect(jsonPath("$.activeBreaches",  is(0)))
                    .andExpect(jsonPath("$.atRiskCount",     is(0)));
        }

        @Test
        @DisplayName("includes lastEtlRun timestamp when ETL has run")
        void includesLastEtlRunTimestamp() throws Exception {
            when(analyticsService.getSlaMetrics()).thenReturn(Collections.emptyList());
            when(analyticsService.getLastEtlRunTime())
                    .thenReturn(OffsetDateTime.parse("2026-03-11T22:30:00Z"));
            when(serviceRequestRepository.countActiveBreaches(anyList())).thenReturn(0L);
            when(serviceRequestRepository.findAtRiskTickets(anyList(), any(), any()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/dashboard/admin/kpis")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lastEtlRun").exists());
        }
    }

    // ── GET /api/v1/dashboard/admin/sla-metrics ───────────────────────────────

    @Nested
    @DisplayName("GET /admin/sla-metrics — SLA metrics")
    class SlaMetrics {

        @Test
        @DisplayName("returns 200 with SLA metric rows from the analytics service")
        void returnsRowsFromService() throws Exception {
            List<SlaMetricEntry> entries = List.of(
                    slaEntry("IT_SUPPORT",  "CRITICAL", 120, 18, 83.64),
                    slaEntry("FACILITIES",  "MEDIUM",   180, 12, 92.90),
                    slaEntry("HR_REQUEST",  "HIGH",      95,  6, 93.50)
            );
            when(analyticsService.getSlaMetrics()).thenReturn(entries);

            mockMvc.perform(get("/api/v1/dashboard/admin/sla-metrics")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].category",          is("IT_SUPPORT")))
                    .andExpect(jsonPath("$[0].priority",          is("CRITICAL")))
                    .andExpect(jsonPath("$[0].totalTickets",      is(120)))
                    .andExpect(jsonPath("$[0].complianceRatePct", is(83.64)));

            verify(analyticsService).getSlaMetrics();
        }

        @Test
        @DisplayName("returns empty array when ETL has not run yet")
        void returnsEmptyWhenEtlNotRun() throws Exception {
            when(analyticsService.getSlaMetrics()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/dashboard/admin/sla-metrics")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ── GET /api/v1/dashboard/admin/daily-volume ──────────────────────────────

    @Nested
    @DisplayName("GET /admin/daily-volume — daily ticket volume")
    class DailyVolume {

        @Test
        @DisplayName("returns 200 with daily volume rows from the analytics service")
        void returnsRowsFromService() throws Exception {
            List<DailyVolumeEntry> entries = List.of(
                    volumeEntry("2026-03-01", "billing",   18),
                    volumeEntry("2026-03-01", "technical", 24),
                    volumeEntry("2026-03-02", "billing",   15),
                    volumeEntry("2026-03-02", "technical", 27)
            );
            when(analyticsService.getDailyVolume()).thenReturn(entries);

            mockMvc.perform(get("/api/v1/dashboard/admin/daily-volume")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(4)))
                    .andExpect(jsonPath("$[0].reportDate",  is("2026-03-01")))
                    .andExpect(jsonPath("$[0].category",    is("billing")))
                    .andExpect(jsonPath("$[0].ticketCount", is(18)));

            verify(analyticsService).getDailyVolume();
        }

        @Test
        @DisplayName("returns empty array when ETL has not run yet")
        void returnsEmptyWhenEtlNotRun() throws Exception {
            when(analyticsService.getDailyVolume()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/dashboard/admin/daily-volume")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ── GET /api/v1/dashboard/admin/agent-leaderboard ────────────────────────

    @Nested
    @DisplayName("GET /admin/agent-leaderboard — agent leaderboard")
    class AgentLeaderboard {

        @Test
        @DisplayName("returns 200 with agent leaderboard from the analytics service")
        void returnsLeaderboard() throws Exception {
            List<AgentLeaderboardEntry> entries = List.of(
                    agentEntry("Kwame Boateng", 95.1),
                    agentEntry("Alice Johnson", 92.5),
                    agentEntry("Bob Mensah",    90.3)
            );
            when(analyticsService.getLeaderboard()).thenReturn(entries);

            mockMvc.perform(get("/api/v1/dashboard/admin/agent-leaderboard")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].agentName",           is("Kwame Boateng")))
                    .andExpect(jsonPath("$[0].slaComplianceRatePct",is(95.1)));

            verify(analyticsService).getLeaderboard();
        }

        @Test
        @DisplayName("returns empty array when ETL has not run yet")
        void returnsEmptyWhenEtlNotRun() throws Exception {
            when(analyticsService.getLeaderboard()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/dashboard/admin/agent-leaderboard")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ── GET /api/v1/dashboard/admin/agent-history ─────────────────────────────

    @Nested
    @DisplayName("GET /admin/agent-history — agent performance history")
    class AgentHistory {

        @Test
        @DisplayName("returns 200 with two-week agent performance history")
        void returnsHistory() throws Exception {
            List<AgentLeaderboardEntry> entries = List.of(
                    agentEntry("Alice Johnson", 91.2),
                    agentEntry("Alice Johnson", 92.5),
                    agentEntry("Bob Mensah",    88.4),
                    agentEntry("Bob Mensah",    90.3)
            );
            when(analyticsService.getAgentPerfHistory()).thenReturn(entries);

            mockMvc.perform(get("/api/v1/dashboard/admin/agent-history")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(4)));

            verify(analyticsService).getAgentPerfHistory();
        }

        @Test
        @DisplayName("returns empty array when ETL has not run yet")
        void returnsEmptyWhenEtlNotRun() throws Exception {
            when(analyticsService.getAgentPerfHistory()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/dashboard/admin/agent-history")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ── GET /api/v1/dashboard/admin/dept-workload ─────────────────────────────

    @Nested
    @DisplayName("GET /admin/dept-workload — department workload")
    class DeptWorkload {

        @Test
        @DisplayName("returns 200 with department workload from the analytics service")
        void returnsWorkload() throws Exception {
            List<DeptWorkloadEntry> entries = List.of(
                    deptEntry("IT Support", 41, 57),
                    deptEntry("Facilities", 20, 26),
                    deptEntry("HR",         17, 24)
            );
            when(analyticsService.getDeptWorkload()).thenReturn(entries);

            mockMvc.perform(get("/api/v1/dashboard/admin/dept-workload")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].departmentName", is("IT Support")))
                    .andExpect(jsonPath("$[0].openTickets",    is(41)))
                    .andExpect(jsonPath("$[0].resolvedTickets",is(57)));

            verify(analyticsService).getDeptWorkload();
        }

        @Test
        @DisplayName("returns empty array when ETL has not run yet")
        void returnsEmptyWhenEtlNotRun() throws Exception {
            when(analyticsService.getDeptWorkload()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/dashboard/admin/dept-workload")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ── GET /api/v1/dashboard/admin/dept-history ──────────────────────────────

    @Nested
    @DisplayName("GET /admin/dept-history — department performance history")
    class DeptHistory {

        @Test
        @DisplayName("returns 200 with two-week department performance history")
        void returnsHistory() throws Exception {
            List<DeptWorkloadEntry> entries = List.of(
                    deptEntry("IT Support", 34, 52),
                    deptEntry("IT Support", 38, 49),
                    deptEntry("IT Support", 41, 57),
                    deptEntry("Facilities", 21, 28),
                    deptEntry("Facilities", 24, 31),
                    deptEntry("Facilities", 20, 26)
            );
            when(analyticsService.getDeptPerfHistory()).thenReturn(entries);

            mockMvc.perform(get("/api/v1/dashboard/admin/dept-history")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(6)));

            verify(analyticsService).getDeptPerfHistory();
        }

        @Test
        @DisplayName("returns empty array when ETL has not run yet")
        void returnsEmptyWhenEtlNotRun() throws Exception {
            when(analyticsService.getDeptPerfHistory()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/dashboard/admin/dept-history")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}

