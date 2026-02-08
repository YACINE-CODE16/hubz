package com.hubz.presentation.controller;

import com.hubz.application.dto.response.*;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.PersonalDashboardService;
import com.hubz.domain.enums.GoalType;
import com.hubz.domain.enums.HabitFrequency;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = PersonalDashboardController.class,
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
@DisplayName("PersonalDashboardController Unit Tests")
class PersonalDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PersonalDashboardService personalDashboardService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

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

    private PersonalDashboardResponse createDashboardResponse() {
        // Create stats
        PersonalDashboardResponse.DashboardStats stats = PersonalDashboardResponse.DashboardStats.builder()
                .totalGoals(5)
                .completedGoals(2)
                .totalTasks(10)
                .completedTasks(4)
                .overdueTasks(1)
                .todayTasksCount(3)
                .totalHabits(4)
                .completedHabitsToday(2)
                .currentStreak(7)
                .upcomingEventsCount(2)
                .build();

        // Create sample tasks
        List<TaskResponse> todayTasks = List.of(
                TaskResponse.builder()
                        .id(UUID.randomUUID())
                        .title("Complete report")
                        .status(TaskStatus.IN_PROGRESS)
                        .priority(TaskPriority.HIGH)
                        .dueDate(LocalDateTime.now())
                        .build()
        );

        // Create sample habits with status
        HabitResponse habitResponse = HabitResponse.builder()
                .id(UUID.randomUUID())
                .name("Morning Exercise")
                .icon("dumbbell")
                .frequency(HabitFrequency.DAILY)
                .build();

        List<PersonalDashboardResponse.HabitWithStatusResponse> todayHabits = List.of(
                PersonalDashboardResponse.HabitWithStatusResponse.builder()
                        .habit(habitResponse)
                        .completedToday(true)
                        .currentStreak(7)
                        .completedLast7Days(6)
                        .build()
        );

        // Create sample events
        List<EventResponse> upcomingEvents = List.of(
                EventResponse.builder()
                        .id(UUID.randomUUID())
                        .title("Team Meeting")
                        .startTime(LocalDateTime.now().plusDays(1))
                        .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                        .build()
        );

        // Create sample goals
        List<GoalResponse> goals = List.of(
                GoalResponse.builder()
                        .id(UUID.randomUUID())
                        .title("Learn Java")
                        .type(GoalType.SHORT)
                        .totalTasks(10)
                        .completedTasks(6)
                        .deadline(LocalDate.now().plusMonths(1))
                        .build()
        );

        return PersonalDashboardResponse.builder()
                .stats(stats)
                .todayTasks(todayTasks)
                .todayHabits(todayHabits)
                .upcomingEvents(upcomingEvents)
                .goals(goals)
                .build();
    }

    @Nested
    @DisplayName("GET /api/users/me/dashboard - Get Personal Dashboard")
    class GetDashboardTests {

        @Test
        @DisplayName("Should return 200 and dashboard data")
        void shouldGetDashboard() throws Exception {
            // Given
            PersonalDashboardResponse response = createDashboardResponse();
            when(personalDashboardService.getDashboard(userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/users/me/dashboard")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stats.totalGoals").value(5))
                    .andExpect(jsonPath("$.stats.completedGoals").value(2))
                    .andExpect(jsonPath("$.stats.totalTasks").value(10))
                    .andExpect(jsonPath("$.stats.currentStreak").value(7))
                    .andExpect(jsonPath("$.todayTasks").isArray())
                    .andExpect(jsonPath("$.todayTasks[0].title").value("Complete report"))
                    .andExpect(jsonPath("$.todayHabits").isArray())
                    .andExpect(jsonPath("$.todayHabits[0].completedToday").value(true))
                    .andExpect(jsonPath("$.upcomingEvents").isArray())
                    .andExpect(jsonPath("$.goals").isArray());

            verify(personalDashboardService).getDashboard(userId);
        }

        @Test
        @DisplayName("Should return 200 with empty dashboard data for new user")
        void shouldReturnEmptyDashboard() throws Exception {
            // Given
            PersonalDashboardResponse.DashboardStats emptyStats = PersonalDashboardResponse.DashboardStats.builder()
                    .totalGoals(0)
                    .completedGoals(0)
                    .totalTasks(0)
                    .completedTasks(0)
                    .overdueTasks(0)
                    .todayTasksCount(0)
                    .totalHabits(0)
                    .completedHabitsToday(0)
                    .currentStreak(0)
                    .upcomingEventsCount(0)
                    .build();

            PersonalDashboardResponse response = PersonalDashboardResponse.builder()
                    .stats(emptyStats)
                    .todayTasks(List.of())
                    .todayHabits(List.of())
                    .upcomingEvents(List.of())
                    .goals(List.of())
                    .build();

            when(personalDashboardService.getDashboard(userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/users/me/dashboard")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stats.totalGoals").value(0))
                    .andExpect(jsonPath("$.stats.totalTasks").value(0))
                    .andExpect(jsonPath("$.todayTasks").isEmpty())
                    .andExpect(jsonPath("$.todayHabits").isEmpty())
                    .andExpect(jsonPath("$.upcomingEvents").isEmpty())
                    .andExpect(jsonPath("$.goals").isEmpty());

            verify(personalDashboardService).getDashboard(userId);
        }

        @Test
        @DisplayName("Should return dashboard with overdue tasks indicator")
        void shouldShowOverdueTasks() throws Exception {
            // Given
            PersonalDashboardResponse.DashboardStats stats = PersonalDashboardResponse.DashboardStats.builder()
                    .totalGoals(0)
                    .completedGoals(0)
                    .totalTasks(5)
                    .completedTasks(2)
                    .overdueTasks(2)
                    .todayTasksCount(3)
                    .totalHabits(0)
                    .completedHabitsToday(0)
                    .currentStreak(0)
                    .upcomingEventsCount(0)
                    .build();

            PersonalDashboardResponse response = PersonalDashboardResponse.builder()
                    .stats(stats)
                    .todayTasks(List.of())
                    .todayHabits(List.of())
                    .upcomingEvents(List.of())
                    .goals(List.of())
                    .build();

            when(personalDashboardService.getDashboard(userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/users/me/dashboard")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stats.overdueTasks").value(2))
                    .andExpect(jsonPath("$.stats.todayTasksCount").value(3));

            verify(personalDashboardService).getDashboard(userId);
        }
    }
}
