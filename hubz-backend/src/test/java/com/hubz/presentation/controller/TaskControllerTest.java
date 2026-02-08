package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateTaskRequest;
import com.hubz.application.dto.request.UpdateTaskRequest;
import com.hubz.application.dto.request.UpdateTaskStatusRequest;
import com.hubz.application.dto.response.TaskResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.TaskService;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.exception.AccessDeniedException;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

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
        value = TaskController.class,
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
@DisplayName("TaskController Unit Tests")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private UUID orgId;
    private UUID taskId;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        taskId = UUID.randomUUID();

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

    private TaskResponse createTaskResponse() {
        return TaskResponse.builder()
                .id(taskId)
                .title("Test Task")
                .description("Test description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .organizationId(orgId)
                .creatorId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/organizations/{orgId}/tasks - Get Tasks By Organization")
    class GetByOrganizationTests {

        @Test
        @DisplayName("Should return 200 and list of tasks")
        void shouldGetTasksByOrganization() throws Exception {
            // Given
            List<TaskResponse> responses = List.of(createTaskResponse());
            when(taskService.getByOrganization(orgId, userId)).thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/tasks", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Test Task"))
                    .andExpect(jsonPath("$[0].status").value("TODO"));

            verify(taskService).getByOrganization(orgId, userId);
        }

        @Test
        @DisplayName("Should return 200 and empty list when no tasks")
        void shouldReturnEmptyList() throws Exception {
            // Given
            when(taskService.getByOrganization(orgId, userId)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/tasks", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());

            verify(taskService).getByOrganization(orgId, userId);
        }
    }

    @Nested
    @DisplayName("POST /api/organizations/{orgId}/tasks - Create Task")
    class CreateTests {

        @Test
        @DisplayName("Should return 201 and task when creation is successful")
        void shouldCreateTask() throws Exception {
            // Given
            TaskResponse response = createTaskResponse();
            when(taskService.create(any(CreateTaskRequest.class), eq(orgId), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/tasks", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Test Task",
                                        "description": "Test description",
                                        "priority": "MEDIUM"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Test Task"))
                    .andExpect(jsonPath("$.status").value("TODO"));

            verify(taskService).create(any(CreateTaskRequest.class), eq(orgId), eq(userId));
        }

        @Test
        @DisplayName("Should return 400 when title is blank")
        void shouldReturn400WhenTitleBlank() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/tasks", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "",
                                        "description": "Test description"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).create(any(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when title is missing")
        void shouldReturn400WhenTitleMissing() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/tasks", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "description": "Test description"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).create(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("PUT /api/tasks/{id} - Update Task")
    class UpdateTests {

        @Test
        @DisplayName("Should return 200 and updated task when successful")
        void shouldUpdateTask() throws Exception {
            // Given
            TaskResponse response = TaskResponse.builder()
                    .id(taskId)
                    .title("Updated Task")
                    .description("Updated description")
                    .status(TaskStatus.IN_PROGRESS)
                    .priority(TaskPriority.HIGH)
                    .organizationId(orgId)
                    .creatorId(userId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(taskService.update(eq(taskId), any(UpdateTaskRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(put("/api/tasks/{id}", taskId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Updated Task",
                                        "description": "Updated description",
                                        "priority": "HIGH"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Task"))
                    .andExpect(jsonPath("$.priority").value("HIGH"));

            verify(taskService).update(eq(taskId), any(UpdateTaskRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 404 when task not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            when(taskService.update(eq(taskId), any(UpdateTaskRequest.class), eq(userId)))
                    .thenThrow(new TaskNotFoundException(taskId));

            // When & Then
            mockMvc.perform(put("/api/tasks/{id}", taskId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Updated Task"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            when(taskService.update(eq(taskId), any(UpdateTaskRequest.class), eq(userId)))
                    .thenThrow(new AccessDeniedException("Not authorized"));

            // When & Then
            mockMvc.perform(put("/api/tasks/{id}", taskId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Updated Task"
                                    }
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /api/tasks/{id}/status - Update Task Status")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should return 200 and updated task when status change is successful")
        void shouldUpdateTaskStatus() throws Exception {
            // Given
            TaskResponse response = TaskResponse.builder()
                    .id(taskId)
                    .title("Test Task")
                    .status(TaskStatus.DONE)
                    .organizationId(orgId)
                    .creatorId(userId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(taskService.updateStatus(eq(taskId), any(UpdateTaskStatusRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "status": "DONE"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DONE"));

            verify(taskService).updateStatus(eq(taskId), any(UpdateTaskStatusRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 404 when task not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            when(taskService.updateStatus(eq(taskId), any(UpdateTaskStatusRequest.class), eq(userId)))
                    .thenThrow(new TaskNotFoundException(taskId));

            // When & Then
            mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "status": "DONE"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/tasks/{id} - Delete Task")
    class DeleteTests {

        @Test
        @DisplayName("Should return 204 when deletion is successful")
        void shouldDeleteTask() throws Exception {
            // Given
            doNothing().when(taskService).delete(taskId, userId);

            // When & Then
            mockMvc.perform(delete("/api/tasks/{id}", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isNoContent());

            verify(taskService).delete(taskId, userId);
        }

        @Test
        @DisplayName("Should return 404 when task not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            doThrow(new TaskNotFoundException(taskId))
                    .when(taskService).delete(taskId, userId);

            // When & Then
            mockMvc.perform(delete("/api/tasks/{id}", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            doThrow(new AccessDeniedException("Not authorized"))
                    .when(taskService).delete(taskId, userId);

            // When & Then
            mockMvc.perform(delete("/api/tasks/{id}", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/users/me/tasks - Get My Tasks")
    class GetMyTasksTests {

        @Test
        @DisplayName("Should return 200 and list of user's tasks")
        void shouldGetMyTasks() throws Exception {
            // Given
            List<TaskResponse> responses = List.of(createTaskResponse());
            when(taskService.getByUser(userId)).thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/users/me/tasks")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Test Task"));

            verify(taskService).getByUser(userId);
        }

        @Test
        @DisplayName("Should return 200 and empty list when user has no tasks")
        void shouldReturnEmptyList() throws Exception {
            // Given
            when(taskService.getByUser(userId)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/users/me/tasks")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());

            verify(taskService).getByUser(userId);
        }
    }
}
