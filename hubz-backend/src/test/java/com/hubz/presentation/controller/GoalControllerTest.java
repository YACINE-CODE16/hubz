package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateGoalRequest;
import com.hubz.application.dto.request.UpdateGoalRequest;
import com.hubz.application.dto.response.GoalAnalyticsResponse;
import com.hubz.application.dto.response.GoalResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.GoalAnalyticsService;
import com.hubz.application.service.GoalService;
import com.hubz.domain.enums.GoalType;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.GoalNotFoundException;
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
        value = GoalController.class,
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
@DisplayName("GoalController Unit Tests")
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoalService goalService;

    @MockBean
    private GoalAnalyticsService goalAnalyticsService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private UUID orgId;
    private UUID goalId;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        goalId = UUID.randomUUID();

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

    private GoalResponse createGoalResponse() {
        return GoalResponse.builder()
                .id(goalId)
                .title("Test Goal")
                .description("Test description")
                .type(GoalType.SHORT)
                .deadline(LocalDate.now().plusDays(30))
                .organizationId(orgId)
                .userId(userId)
                .totalTasks(5)
                .completedTasks(2)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/organizations/{orgId}/goals - Get Organization Goals")
    class GetByOrganizationTests {

        @Test
        @DisplayName("Should return 200 and list of goals")
        void shouldGetGoalsByOrganization() throws Exception {
            // Given
            List<GoalResponse> responses = List.of(createGoalResponse());
            when(goalService.getByOrganization(orgId, userId)).thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/goals", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Test Goal"))
                    .andExpect(jsonPath("$[0].type").value("SHORT"));

            verify(goalService).getByOrganization(orgId, userId);
        }

        @Test
        @DisplayName("Should return 200 and empty list when no goals")
        void shouldReturnEmptyList() throws Exception {
            // Given
            when(goalService.getByOrganization(orgId, userId)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/goals", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());

            verify(goalService).getByOrganization(orgId, userId);
        }
    }

    @Nested
    @DisplayName("POST /api/organizations/{orgId}/goals - Create Organization Goal")
    class CreateOrganizationGoalTests {

        @Test
        @DisplayName("Should return 201 and goal when creation is successful")
        void shouldCreateGoal() throws Exception {
            // Given
            GoalResponse response = createGoalResponse();
            when(goalService.create(any(CreateGoalRequest.class), eq(orgId), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/goals", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Test Goal",
                                        "description": "Test description",
                                        "type": "SHORT"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Test Goal"))
                    .andExpect(jsonPath("$.type").value("SHORT"));

            verify(goalService).create(any(CreateGoalRequest.class), eq(orgId), eq(userId));
        }

        @Test
        @DisplayName("Should return 400 when title is blank")
        void shouldReturn400WhenTitleBlank() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/goals", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "",
                                        "type": "SHORT"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(goalService, never()).create(any(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when type is missing")
        void shouldReturn400WhenTypeMissing() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/goals", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Test Goal"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(goalService, never()).create(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("GET /api/users/me/goals - Get Personal Goals")
    class GetPersonalGoalsTests {

        @Test
        @DisplayName("Should return 200 and list of personal goals")
        void shouldGetPersonalGoals() throws Exception {
            // Given
            GoalResponse response = GoalResponse.builder()
                    .id(goalId)
                    .title("Personal Goal")
                    .type(GoalType.MEDIUM)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(goalService.getPersonalGoals(userId)).thenReturn(List.of(response));

            // When & Then
            mockMvc.perform(get("/api/users/me/goals")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Personal Goal"));

            verify(goalService).getPersonalGoals(userId);
        }
    }

    @Nested
    @DisplayName("POST /api/users/me/goals - Create Personal Goal")
    class CreatePersonalGoalTests {

        @Test
        @DisplayName("Should return 201 and personal goal when creation is successful")
        void shouldCreatePersonalGoal() throws Exception {
            // Given
            GoalResponse response = GoalResponse.builder()
                    .id(goalId)
                    .title("Personal Goal")
                    .type(GoalType.LONG)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(goalService.create(any(CreateGoalRequest.class), eq(null), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/users/me/goals")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Personal Goal",
                                        "type": "LONG"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Personal Goal"));

            verify(goalService).create(any(CreateGoalRequest.class), eq(null), eq(userId));
        }
    }

    @Nested
    @DisplayName("PUT /api/goals/{id} - Update Goal")
    class UpdateTests {

        @Test
        @DisplayName("Should return 200 and updated goal when successful")
        void shouldUpdateGoal() throws Exception {
            // Given
            GoalResponse response = GoalResponse.builder()
                    .id(goalId)
                    .title("Updated Goal")
                    .type(GoalType.MEDIUM)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(goalService.update(eq(goalId), any(UpdateGoalRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(put("/api/goals/{id}", goalId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Updated Goal",
                                        "type": "MEDIUM"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Goal"));

            verify(goalService).update(eq(goalId), any(UpdateGoalRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 404 when goal not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            when(goalService.update(eq(goalId), any(UpdateGoalRequest.class), eq(userId)))
                    .thenThrow(new GoalNotFoundException(goalId));

            // When & Then
            mockMvc.perform(put("/api/goals/{id}", goalId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Updated Goal",
                                        "type": "SHORT"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/goals/{id} - Delete Goal")
    class DeleteTests {

        @Test
        @DisplayName("Should return 204 when deletion is successful")
        void shouldDeleteGoal() throws Exception {
            // Given
            doNothing().when(goalService).delete(goalId, userId);

            // When & Then
            mockMvc.perform(delete("/api/goals/{id}", goalId)
                            .principal(mockAuth))
                    .andExpect(status().isNoContent());

            verify(goalService).delete(goalId, userId);
        }

        @Test
        @DisplayName("Should return 404 when goal not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            doThrow(new GoalNotFoundException(goalId))
                    .when(goalService).delete(goalId, userId);

            // When & Then
            mockMvc.perform(delete("/api/goals/{id}", goalId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            doThrow(new AccessDeniedException("Not authorized"))
                    .when(goalService).delete(goalId, userId);

            // When & Then
            mockMvc.perform(delete("/api/goals/{id}", goalId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/users/me/goals/analytics - Get Personal Goal Analytics")
    class GetPersonalAnalyticsTests {

        @Test
        @DisplayName("Should return 200 and analytics")
        void shouldGetPersonalGoalAnalytics() throws Exception {
            // Given
            GoalAnalyticsResponse response = GoalAnalyticsResponse.builder()
                    .totalGoals(10)
                    .completedGoals(5)
                    .build();

            when(goalAnalyticsService.getPersonalAnalytics(userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/users/me/goals/analytics")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalGoals").value(10))
                    .andExpect(jsonPath("$.completedGoals").value(5));

            verify(goalAnalyticsService).getPersonalAnalytics(userId);
        }
    }

    @Nested
    @DisplayName("GET /api/organizations/{orgId}/goals/analytics - Get Organization Goal Analytics")
    class GetOrganizationAnalyticsTests {

        @Test
        @DisplayName("Should return 200 and analytics")
        void shouldGetOrganizationGoalAnalytics() throws Exception {
            // Given
            GoalAnalyticsResponse response = GoalAnalyticsResponse.builder()
                    .totalGoals(20)
                    .completedGoals(15)
                    .build();

            when(goalAnalyticsService.getOrganizationAnalytics(orgId, userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/goals/analytics", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalGoals").value(20))
                    .andExpect(jsonPath("$.completedGoals").value(15));

            verify(goalAnalyticsService).getOrganizationAnalytics(orgId, userId);
        }
    }
}
