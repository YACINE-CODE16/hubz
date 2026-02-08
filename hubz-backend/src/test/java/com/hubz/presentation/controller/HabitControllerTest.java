package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateHabitRequest;
import com.hubz.application.dto.request.LogHabitRequest;
import com.hubz.application.dto.request.UpdateHabitRequest;
import com.hubz.application.dto.response.HabitAnalyticsResponse;
import com.hubz.application.dto.response.HabitLogResponse;
import com.hubz.application.dto.response.HabitResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.HabitAnalyticsService;
import com.hubz.application.service.HabitService;
import com.hubz.domain.enums.HabitFrequency;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.HabitNotFoundException;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = HabitController.class,
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
@DisplayName("HabitController Unit Tests")
class HabitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HabitService habitService;

    @MockBean
    private HabitAnalyticsService habitAnalyticsService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private UUID habitId;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        habitId = UUID.randomUUID();

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

    private HabitResponse createHabitResponse() {
        return HabitResponse.builder()
                .id(habitId)
                .name("Morning Exercise")
                .icon("dumbbell")
                .frequency(HabitFrequency.DAILY)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/users/me/habits - Get User Habits")
    class GetUserHabitsTests {

        @Test
        @DisplayName("Should return 200 and list of habits")
        void shouldGetUserHabits() throws Exception {
            // Given
            List<HabitResponse> responses = List.of(createHabitResponse());
            when(habitService.getUserHabits(userId)).thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/users/me/habits")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Morning Exercise"))
                    .andExpect(jsonPath("$[0].frequency").value("DAILY"));

            verify(habitService).getUserHabits(userId);
        }

        @Test
        @DisplayName("Should return 200 and empty list when no habits")
        void shouldReturnEmptyList() throws Exception {
            // Given
            when(habitService.getUserHabits(userId)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/users/me/habits")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());

            verify(habitService).getUserHabits(userId);
        }
    }

    @Nested
    @DisplayName("POST /api/users/me/habits - Create Habit")
    class CreateHabitTests {

        @Test
        @DisplayName("Should return 201 and habit when creation is successful")
        void shouldCreateHabit() throws Exception {
            // Given
            HabitResponse response = createHabitResponse();
            when(habitService.create(any(CreateHabitRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/users/me/habits")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Morning Exercise",
                                        "icon": "dumbbell",
                                        "frequency": "DAILY"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Morning Exercise"))
                    .andExpect(jsonPath("$.frequency").value("DAILY"));

            verify(habitService).create(any(CreateHabitRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 400 when name is blank")
        void shouldReturn400WhenNameBlank() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/users/me/habits")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "",
                                        "icon": "dumbbell",
                                        "frequency": "DAILY"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(habitService, never()).create(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when icon is blank")
        void shouldReturn400WhenIconBlank() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/users/me/habits")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Morning Exercise",
                                        "icon": "",
                                        "frequency": "DAILY"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(habitService, never()).create(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when frequency is missing")
        void shouldReturn400WhenFrequencyMissing() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/users/me/habits")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Morning Exercise",
                                        "icon": "dumbbell"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(habitService, never()).create(any(), any());
        }
    }

    @Nested
    @DisplayName("PUT /api/habits/{id} - Update Habit")
    class UpdateHabitTests {

        @Test
        @DisplayName("Should return 200 and updated habit when successful")
        void shouldUpdateHabit() throws Exception {
            // Given
            HabitResponse response = HabitResponse.builder()
                    .id(habitId)
                    .name("Evening Meditation")
                    .icon("lotus")
                    .frequency(HabitFrequency.DAILY)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(habitService.update(eq(habitId), any(UpdateHabitRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(put("/api/habits/{id}", habitId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Evening Meditation",
                                        "icon": "lotus",
                                        "frequency": "DAILY"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Evening Meditation"));

            verify(habitService).update(eq(habitId), any(UpdateHabitRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 404 when habit not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            when(habitService.update(eq(habitId), any(UpdateHabitRequest.class), eq(userId)))
                    .thenThrow(new HabitNotFoundException(habitId));

            // When & Then
            mockMvc.perform(put("/api/habits/{id}", habitId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Evening Meditation",
                                        "icon": "lotus",
                                        "frequency": "DAILY"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/habits/{id} - Delete Habit")
    class DeleteHabitTests {

        @Test
        @DisplayName("Should return 204 when deletion is successful")
        void shouldDeleteHabit() throws Exception {
            // Given
            doNothing().when(habitService).delete(habitId, userId);

            // When & Then
            mockMvc.perform(delete("/api/habits/{id}", habitId)
                            .principal(mockAuth))
                    .andExpect(status().isNoContent());

            verify(habitService).delete(habitId, userId);
        }

        @Test
        @DisplayName("Should return 404 when habit not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            doThrow(new HabitNotFoundException(habitId))
                    .when(habitService).delete(habitId, userId);

            // When & Then
            mockMvc.perform(delete("/api/habits/{id}", habitId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            doThrow(new AccessDeniedException("Not authorized"))
                    .when(habitService).delete(habitId, userId);

            // When & Then
            mockMvc.perform(delete("/api/habits/{id}", habitId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/habits/{id}/log - Log Habit")
    class LogHabitTests {

        @Test
        @DisplayName("Should return 201 when habit is logged successfully")
        void shouldLogHabit() throws Exception {
            // Given
            HabitLogResponse response = HabitLogResponse.builder()
                    .id(UUID.randomUUID())
                    .habitId(habitId)
                    .date(LocalDate.now())
                    .completed(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(habitService.logHabit(eq(habitId), any(LogHabitRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/habits/{id}/log", habitId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "date": "2026-02-02",
                                        "completed": true
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.completed").value(true));

            verify(habitService).logHabit(eq(habitId), any(LogHabitRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 404 when habit not found")
        void shouldReturn404WhenHabitNotFound() throws Exception {
            // Given
            when(habitService.logHabit(eq(habitId), any(LogHabitRequest.class), eq(userId)))
                    .thenThrow(new HabitNotFoundException(habitId));

            // When & Then
            mockMvc.perform(post("/api/habits/{id}/log", habitId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "date": "2026-02-02",
                                        "completed": true
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/habits/{id}/logs - Get Habit Logs")
    class GetHabitLogsTests {

        @Test
        @DisplayName("Should return 200 and list of logs")
        void shouldGetHabitLogs() throws Exception {
            // Given
            HabitLogResponse log = HabitLogResponse.builder()
                    .id(UUID.randomUUID())
                    .habitId(habitId)
                    .date(LocalDate.now())
                    .completed(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(habitService.getHabitLogs(habitId, userId)).thenReturn(List.of(log));

            // When & Then
            mockMvc.perform(get("/api/habits/{id}/logs", habitId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].completed").value(true));

            verify(habitService).getHabitLogs(habitId, userId);
        }
    }

    @Nested
    @DisplayName("GET /api/users/me/habits/analytics - Get Habit Analytics")
    class GetHabitAnalyticsTests {

        @Test
        @DisplayName("Should return 200 and analytics")
        void shouldGetHabitAnalytics() throws Exception {
            // Given
            HabitAnalyticsResponse response = HabitAnalyticsResponse.builder()
                    .totalHabits(5)
                    .dailyCompletionRate(0.85)
                    .weeklyCompletionRate(0.90)
                    .build();

            when(habitAnalyticsService.getAnalytics(userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/users/me/habits/analytics")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalHabits").value(5))
                    .andExpect(jsonPath("$.dailyCompletionRate").value(0.85));

            verify(habitAnalyticsService).getAnalytics(userId);
        }
    }
}
