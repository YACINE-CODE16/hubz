package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateTaskCommentRequest;
import com.hubz.application.dto.request.UpdateTaskCommentRequest;
import com.hubz.application.dto.response.TaskCommentResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.TaskCommentService;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.TaskCommentNotFoundException;
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
        value = TaskCommentController.class,
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
@DisplayName("TaskCommentController Unit Tests")
class TaskCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskCommentService commentService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private UUID taskId;
    private UUID commentId;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        commentId = UUID.randomUUID();

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

    private TaskCommentResponse createCommentResponse() {
        return TaskCommentResponse.builder()
                .id(commentId)
                .taskId(taskId)
                .authorId(userId)
                .authorName("John Doe")
                .content("This is a test comment")
                .parentCommentId(null)
                .replies(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .edited(false)
                .build();
    }

    @Nested
    @DisplayName("GET /api/tasks/{taskId}/comments - Get Comments")
    class GetCommentsTests {

        @Test
        @DisplayName("Should return 200 and list of comments")
        void shouldGetComments() throws Exception {
            // Given
            List<TaskCommentResponse> responses = List.of(createCommentResponse());
            when(commentService.getCommentsByTask(taskId, userId)).thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].content").value("This is a test comment"))
                    .andExpect(jsonPath("$[0].authorName").value("John Doe"));

            verify(commentService).getCommentsByTask(taskId, userId);
        }

        @Test
        @DisplayName("Should return 200 and empty list when no comments")
        void shouldReturnEmptyList() throws Exception {
            // Given
            when(commentService.getCommentsByTask(taskId, userId)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());

            verify(commentService).getCommentsByTask(taskId, userId);
        }

        @Test
        @DisplayName("Should return 404 when task not found")
        void shouldReturn404WhenTaskNotFound() throws Exception {
            // Given
            when(commentService.getCommentsByTask(taskId, userId))
                    .thenThrow(new TaskNotFoundException(taskId));

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/{taskId}/comments/count - Get Comment Count")
    class GetCommentCountTests {

        @Test
        @DisplayName("Should return 200 and comment count")
        void shouldGetCommentCount() throws Exception {
            // Given
            when(commentService.getCommentCount(taskId, userId)).thenReturn(5);

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/comments/count", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(5));

            verify(commentService).getCommentCount(taskId, userId);
        }

        @Test
        @DisplayName("Should return 0 when no comments")
        void shouldReturnZeroCount() throws Exception {
            // Given
            when(commentService.getCommentCount(taskId, userId)).thenReturn(0);

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/comments/count", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(0));

            verify(commentService).getCommentCount(taskId, userId);
        }
    }

    @Nested
    @DisplayName("POST /api/tasks/{taskId}/comments - Create Comment")
    class CreateCommentTests {

        @Test
        @DisplayName("Should return 201 and comment when creation is successful")
        void shouldCreateComment() throws Exception {
            // Given
            TaskCommentResponse response = createCommentResponse();
            when(commentService.createComment(eq(taskId), any(CreateTaskCommentRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": "This is a test comment"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content").value("This is a test comment"));

            verify(commentService).createComment(eq(taskId), any(CreateTaskCommentRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 201 when creating a reply")
        void shouldCreateReply() throws Exception {
            // Given
            UUID parentCommentId = UUID.randomUUID();
            TaskCommentResponse response = TaskCommentResponse.builder()
                    .id(commentId)
                    .taskId(taskId)
                    .authorId(userId)
                    .authorName("John Doe")
                    .content("This is a reply")
                    .parentCommentId(parentCommentId)
                    .replies(List.of())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .edited(false)
                    .build();

            when(commentService.createComment(eq(taskId), any(CreateTaskCommentRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "content": "This is a reply",
                                        "parentCommentId": "%s"
                                    }
                                    """, parentCommentId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content").value("This is a reply"))
                    .andExpect(jsonPath("$.parentCommentId").value(parentCommentId.toString()));

            verify(commentService).createComment(eq(taskId), any(CreateTaskCommentRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 400 when content is blank")
        void shouldReturn400WhenContentBlank() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(commentService, never()).createComment(any(), any(), any());
        }

        @Test
        @DisplayName("Should return 404 when task not found")
        void shouldReturn404WhenTaskNotFound() throws Exception {
            // Given
            when(commentService.createComment(eq(taskId), any(CreateTaskCommentRequest.class), eq(userId)))
                    .thenThrow(new TaskNotFoundException(taskId));

            // When & Then
            mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": "This is a test comment"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/tasks/{taskId}/comments/{commentId} - Update Comment")
    class UpdateCommentTests {

        @Test
        @DisplayName("Should return 200 and updated comment when successful")
        void shouldUpdateComment() throws Exception {
            // Given
            TaskCommentResponse response = TaskCommentResponse.builder()
                    .id(commentId)
                    .taskId(taskId)
                    .authorId(userId)
                    .authorName("John Doe")
                    .content("Updated comment")
                    .createdAt(LocalDateTime.now().minusHours(1))
                    .updatedAt(LocalDateTime.now())
                    .edited(true)
                    .build();

            when(commentService.updateComment(eq(commentId), any(UpdateTaskCommentRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(put("/api/tasks/{taskId}/comments/{commentId}", taskId, commentId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": "Updated comment"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("Updated comment"))
                    .andExpect(jsonPath("$.edited").value(true));

            verify(commentService).updateComment(eq(commentId), any(UpdateTaskCommentRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 404 when comment not found")
        void shouldReturn404WhenCommentNotFound() throws Exception {
            // Given
            when(commentService.updateComment(eq(commentId), any(UpdateTaskCommentRequest.class), eq(userId)))
                    .thenThrow(new TaskCommentNotFoundException(commentId));

            // When & Then
            mockMvc.perform(put("/api/tasks/{taskId}/comments/{commentId}", taskId, commentId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": "Updated comment"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            when(commentService.updateComment(eq(commentId), any(UpdateTaskCommentRequest.class), eq(userId)))
                    .thenThrow(new AccessDeniedException("Not authorized"));

            // When & Then
            mockMvc.perform(put("/api/tasks/{taskId}/comments/{commentId}", taskId, commentId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": "Updated comment"
                                    }
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/tasks/{taskId}/comments/{commentId} - Delete Comment")
    class DeleteCommentTests {

        @Test
        @DisplayName("Should return 204 when deletion is successful")
        void shouldDeleteComment() throws Exception {
            // Given
            doNothing().when(commentService).deleteComment(commentId, userId);

            // When & Then
            mockMvc.perform(delete("/api/tasks/{taskId}/comments/{commentId}", taskId, commentId)
                            .principal(mockAuth))
                    .andExpect(status().isNoContent());

            verify(commentService).deleteComment(commentId, userId);
        }

        @Test
        @DisplayName("Should return 404 when comment not found")
        void shouldReturn404WhenCommentNotFound() throws Exception {
            // Given
            doThrow(new TaskCommentNotFoundException(commentId))
                    .when(commentService).deleteComment(commentId, userId);

            // When & Then
            mockMvc.perform(delete("/api/tasks/{taskId}/comments/{commentId}", taskId, commentId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            doThrow(new AccessDeniedException("Not authorized"))
                    .when(commentService).deleteComment(commentId, userId);

            // When & Then
            mockMvc.perform(delete("/api/tasks/{taskId}/comments/{commentId}", taskId, commentId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }
}
