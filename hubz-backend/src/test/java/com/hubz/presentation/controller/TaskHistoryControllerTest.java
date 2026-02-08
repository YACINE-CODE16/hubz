package com.hubz.presentation.controller;

import com.hubz.application.dto.response.TaskHistoryResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.TaskHistoryService;
import com.hubz.domain.enums.TaskHistoryField;
import com.hubz.domain.exception.TaskNotFoundException;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = TaskHistoryController.class,
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
@DisplayName("TaskHistoryController Tests")
class TaskHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskHistoryService taskHistoryService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID taskId;
    private UUID userId;
    private User testUser;
    private Authentication mockAuth;
    private TaskHistoryResponse testHistoryResponse;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
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

        testHistoryResponse = TaskHistoryResponse.builder()
                .id(UUID.randomUUID())
                .taskId(taskId)
                .userId(userId)
                .userName("John Doe")
                .userPhotoUrl("https://example.com/photo.jpg")
                .fieldChanged(TaskHistoryField.STATUS)
                .oldValue("TODO")
                .newValue("IN_PROGRESS")
                .changedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/tasks/{taskId}/history")
    class GetTaskHistoryTests {

        @Test
        @DisplayName("Should return task history")
        void shouldReturnTaskHistory() throws Exception {
            // Given
            when(taskHistoryService.getTaskHistory(taskId, userId))
                    .thenReturn(List.of(testHistoryResponse));

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/history", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].taskId").value(taskId.toString()))
                    .andExpect(jsonPath("$[0].fieldChanged").value("STATUS"))
                    .andExpect(jsonPath("$[0].oldValue").value("TODO"))
                    .andExpect(jsonPath("$[0].newValue").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$[0].userName").value("John Doe"));
        }

        @Test
        @DisplayName("Should return empty list when no history")
        void shouldReturnEmptyListWhenNoHistory() throws Exception {
            // Given
            when(taskHistoryService.getTaskHistory(taskId, userId))
                    .thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/history", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Should filter by field when field parameter provided")
        void shouldFilterByField() throws Exception {
            // Given
            when(taskHistoryService.getTaskHistoryByField(eq(taskId), eq(TaskHistoryField.STATUS), eq(userId)))
                    .thenReturn(List.of(testHistoryResponse));

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/history", taskId)
                            .principal(mockAuth)
                            .param("field", "STATUS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].fieldChanged").value("STATUS"));
        }

        @Test
        @DisplayName("Should return 404 when task not found")
        void shouldReturn404WhenTaskNotFound() throws Exception {
            // Given
            UUID nonExistentTaskId = UUID.randomUUID();
            when(taskHistoryService.getTaskHistory(nonExistentTaskId, userId))
                    .thenThrow(new TaskNotFoundException(nonExistentTaskId));

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/history", nonExistentTaskId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return multiple history entries")
        void shouldReturnMultipleHistoryEntries() throws Exception {
            // Given
            TaskHistoryResponse statusChange = TaskHistoryResponse.builder()
                    .id(UUID.randomUUID())
                    .taskId(taskId)
                    .userId(userId)
                    .userName("John Doe")
                    .fieldChanged(TaskHistoryField.STATUS)
                    .oldValue("TODO")
                    .newValue("IN_PROGRESS")
                    .changedAt(LocalDateTime.now())
                    .build();

            TaskHistoryResponse priorityChange = TaskHistoryResponse.builder()
                    .id(UUID.randomUUID())
                    .taskId(taskId)
                    .userId(userId)
                    .userName("John Doe")
                    .fieldChanged(TaskHistoryField.PRIORITY)
                    .oldValue("LOW")
                    .newValue("HIGH")
                    .changedAt(LocalDateTime.now().minusMinutes(5))
                    .build();

            when(taskHistoryService.getTaskHistory(taskId, userId))
                    .thenReturn(List.of(statusChange, priorityChange));

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/history", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].fieldChanged").value("STATUS"))
                    .andExpect(jsonPath("$[1].fieldChanged").value("PRIORITY"));
        }
    }
}
