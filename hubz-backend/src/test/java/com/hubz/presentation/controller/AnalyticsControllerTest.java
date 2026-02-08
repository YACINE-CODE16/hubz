package com.hubz.presentation.controller;

import com.hubz.application.dto.request.AnalyticsFilterRequest;
import com.hubz.application.dto.response.*;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.AnalyticsService;
import com.hubz.application.service.CalendarAnalyticsService;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.OrganizationNotFoundException;
import com.hubz.domain.model.User;
import com.hubz.infrastructure.config.CorsProperties;
import com.hubz.infrastructure.security.JwtAuthenticationFilter;
import com.hubz.infrastructure.security.JwtService;
import com.hubz.presentation.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AnalyticsController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, JwtService.class, CorsProperties.class}
        )
)
@Import(GlobalExceptionHandler.class)
@DisplayName("AnalyticsController Unit Tests")
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private CalendarAnalyticsService calendarAnalyticsService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private UUID orgId;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        mockAuth = mock(Authentication.class);
        when(mockAuth.getName()).thenReturn("test@example.com");
        when(userRepositoryPort.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }

    @Nested
    @DisplayName("GET /api/organizations/{orgId}/analytics/tasks - Get Task Analytics")
    class TaskAnalyticsTests {

        @Test
        @DisplayName("Should return 200 and task analytics")
        void shouldGetTaskAnalytics() throws Exception {
            // Given
            TaskAnalyticsResponse response = TaskAnalyticsResponse.builder()
                    .totalTasks(100)
                    .todoCount(30)
                    .inProgressCount(40)
                    .doneCount(30)
                    .completionRate(30.0)
                    .overdueCount(5)
                    .overdueRate(5.0)
                    .tasksByPriority(Map.of("HIGH", 20L, "MEDIUM", 50L, "LOW", 30L))
                    .tasksByStatus(Map.of("TODO", 30L, "IN_PROGRESS", 40L, "DONE", 30L))
                    .tasksCreatedOverTime(List.of())
                    .tasksCompletedOverTime(List.of())
                    .burndownChart(List.of())
                    .velocityChart(List.of())
                    .cumulativeFlowDiagram(List.of())
                    .build();

            when(analyticsService.getTaskAnalytics(eq(orgId), eq(userId), any())).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/analytics/tasks", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTasks").value(100))
                    .andExpect(jsonPath("$.todoCount").value(30))
                    .andExpect(jsonPath("$.inProgressCount").value(40))
                    .andExpect(jsonPath("$.doneCount").value(30))
                    .andExpect(jsonPath("$.completionRate").value(30.0));

            verify(analyticsService).getTaskAnalytics(eq(orgId), eq(userId), any());
        }

        @Test
        @DisplayName("Should return 404 when organization not found")
        void shouldReturn404WhenOrgNotFound() throws Exception {
            // Given
            when(analyticsService.getTaskAnalytics(eq(orgId), eq(userId), any()))
                    .thenThrow(new OrganizationNotFoundException(orgId));

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/analytics/tasks", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            when(analyticsService.getTaskAnalytics(eq(orgId), eq(userId), any()))
                    .thenThrow(new AccessDeniedException("Not authorized"));

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/analytics/tasks", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should pass filter parameters to service when provided")
        void shouldPassFilterParametersToService() throws Exception {
            // Given
            TaskAnalyticsResponse response = TaskAnalyticsResponse.builder()
                    .totalTasks(10)
                    .todoCount(3)
                    .inProgressCount(4)
                    .doneCount(3)
                    .completionRate(30.0)
                    .overdueCount(0)
                    .overdueRate(0.0)
                    .tasksByPriority(Map.of("HIGH", 10L))
                    .tasksByStatus(Map.of("TODO", 3L, "IN_PROGRESS", 4L, "DONE", 3L))
                    .tasksCreatedOverTime(List.of())
                    .tasksCompletedOverTime(List.of())
                    .burndownChart(List.of())
                    .velocityChart(List.of())
                    .cumulativeFlowDiagram(List.of())
                    .build();

            when(analyticsService.getTaskAnalytics(eq(orgId), eq(userId), any(AnalyticsFilterRequest.class)))
                    .thenReturn(response);

            UUID memberId = UUID.randomUUID();

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/analytics/tasks", orgId)
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-01-31")
                            .param("memberIds", memberId.toString())
                            .param("statuses", "TODO", "IN_PROGRESS")
                            .param("priorities", "HIGH")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTasks").value(10));

            verify(analyticsService).getTaskAnalytics(eq(orgId), eq(userId), any(AnalyticsFilterRequest.class));
        }

        @Test
        @DisplayName("Should pass null filters when no params provided")
        void shouldPassNullFiltersWhenNoParamsProvided() throws Exception {
            // Given
            TaskAnalyticsResponse response = TaskAnalyticsResponse.builder()
                    .totalTasks(50)
                    .todoCount(10)
                    .inProgressCount(20)
                    .doneCount(20)
                    .completionRate(40.0)
                    .overdueCount(2)
                    .overdueRate(4.0)
                    .tasksByPriority(Map.of())
                    .tasksByStatus(Map.of("TODO", 10L, "IN_PROGRESS", 20L, "DONE", 20L))
                    .tasksCreatedOverTime(List.of())
                    .tasksCompletedOverTime(List.of())
                    .burndownChart(List.of())
                    .velocityChart(List.of())
                    .cumulativeFlowDiagram(List.of())
                    .build();

            // No filter params => buildFilters returns null
            when(analyticsService.getTaskAnalytics(eq(orgId), eq(userId), eq(null)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/analytics/tasks", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTasks").value(50));

            verify(analyticsService).getTaskAnalytics(eq(orgId), eq(userId), eq(null));
        }
    }

    @Nested
    @DisplayName("GET /api/organizations/{orgId}/analytics/members - Get Member Analytics")
    class MemberAnalyticsTests {

        @Test
        @DisplayName("Should return 200 and member analytics")
        void shouldGetMemberAnalytics() throws Exception {
            // Given
            MemberAnalyticsResponse response = MemberAnalyticsResponse.builder()
                    .memberProductivity(List.of())
                    .memberWorkload(List.of())
                    .topPerformers(List.of())
                    .overloadedMembers(List.of())
                    .activityHeatmap(List.of())
                    .build();

            when(analyticsService.getMemberAnalytics(eq(orgId), eq(userId), any())).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/analytics/members", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberProductivity").isArray())
                    .andExpect(jsonPath("$.memberWorkload").isArray());

            verify(analyticsService).getMemberAnalytics(eq(orgId), eq(userId), any());
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            when(analyticsService.getMemberAnalytics(eq(orgId), eq(userId), any()))
                    .thenThrow(new AccessDeniedException("Not authorized"));

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/analytics/members", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/organizations/{orgId}/analytics/goals - Get Goal Analytics")
    class GoalAnalyticsTests {

        @Test
        @DisplayName("Should return 200 and goal analytics")
        void shouldGetGoalAnalytics() throws Exception {
            // Given
            GoalAnalyticsResponse response = GoalAnalyticsResponse.builder()
                    .totalGoals(10)
                    .completedGoals(3)
                    .inProgressGoals(5)
                    .atRiskGoals(2)
                    .overallProgressPercentage(45.0)
                    .goalCompletionRate(30.0)
                    .goalsByType(Map.of("SHORT", 3L, "MEDIUM", 4L, "LONG", 3L))
                    .avgProgressByType(Map.of("SHORT", 60.0, "MEDIUM", 45.0, "LONG", 30.0))
                    .goalProgressList(List.of())
                    .goalsAtRisk(List.of())
                    .completionHistory(List.of())
                    .goalsOnTrack(5)
                    .goalsBehindSchedule(2)
                    .averageVelocity(1.5)
                    .build();

            when(analyticsService.getGoalAnalytics(eq(orgId), eq(userId), any())).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/analytics/goals", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalGoals").value(10))
                    .andExpect(jsonPath("$.completedGoals").value(3))
                    .andExpect(jsonPath("$.atRiskGoals").value(2));

            verify(analyticsService).getGoalAnalytics(eq(orgId), eq(userId), any());
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            when(analyticsService.getGoalAnalytics(eq(orgId), eq(userId), any()))
                    .thenThrow(new AccessDeniedException("Not authorized"));

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/analytics/goals", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/organizations/{orgId}/analytics - Get Organization Analytics")
    class OrganizationAnalyticsTests {

        @Test
        @DisplayName("Should return 200 and organization analytics")
        void shouldGetOrganizationAnalytics() throws Exception {
            // Given
            OrganizationAnalyticsResponse response = OrganizationAnalyticsResponse.builder()
                    .healthScore(85)
                    .totalMembers(20)
                    .activeMembers(15)
                    .totalTasks(100)
                    .activeTasks(70)
                    .totalGoals(10)
                    .totalEvents(25)
                    .totalNotes(50)
                    .totalTeams(5)
                    .tasksCreatedThisWeek(10)
                    .tasksCompletedThisWeek(8)
                    .eventsThisWeek(3)
                    .taskCompletionTrend(10.0)
                    .memberActivityTrend(5.0)
                    .recentActivity(List.of())
                    .teamPerformance(List.of())
                    .monthlyGrowth(List.of())
                    .build();

            when(analyticsService.getOrganizationAnalytics(orgId, userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/analytics", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.healthScore").value(85))
                    .andExpect(jsonPath("$.totalMembers").value(20))
                    .andExpect(jsonPath("$.activeMembers").value(15));

            verify(analyticsService).getOrganizationAnalytics(orgId, userId);
        }

        @Test
        @DisplayName("Should return 404 when organization not found")
        void shouldReturn404WhenOrgNotFound() throws Exception {
            // Given
            when(analyticsService.getOrganizationAnalytics(orgId, userId))
                    .thenThrow(new OrganizationNotFoundException(orgId));

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/analytics", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/users/me/analytics/habits - Get Habit Analytics")
    class HabitAnalyticsTests {

        @Test
        @DisplayName("Should return 200 and habit analytics")
        void shouldGetHabitAnalytics() throws Exception {
            // Given
            HabitAnalyticsResponse response = HabitAnalyticsResponse.builder()
                    .totalHabits(5)
                    .dailyCompletionRate(80.0)
                    .weeklyCompletionRate(75.0)
                    .monthlyCompletionRate(70.0)
                    .longestStreak(30)
                    .currentStreak(10)
                    .bestStreakHabitName("Morning Exercise")
                    .habitStats(List.of())
                    .completionHeatmap(List.of())
                    .completionByDayOfWeek(Map.of("Monday", 85.0, "Tuesday", 80.0))
                    .last30DaysTrend(List.of())
                    .last90DaysTrend(List.of())
                    .build();

            when(analyticsService.getHabitAnalytics(userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/users/me/analytics/habits")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalHabits").value(5))
                    .andExpect(jsonPath("$.dailyCompletionRate").value(80.0))
                    .andExpect(jsonPath("$.longestStreak").value(30))
                    .andExpect(jsonPath("$.currentStreak").value(10));

            verify(analyticsService).getHabitAnalytics(userId);
        }
    }
}
