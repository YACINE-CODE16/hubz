package com.hubz.presentation.controller;

import com.hubz.application.dto.response.TaskAttachmentResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.TaskAttachmentService;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.TaskAttachmentNotFoundException;
import com.hubz.domain.exception.TaskNotFoundException;
import com.hubz.domain.model.TaskAttachment;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = TaskAttachmentController.class,
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
@DisplayName("TaskAttachmentController Unit Tests")
class TaskAttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskAttachmentService attachmentService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private UUID taskId;
    private UUID attachmentId;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        attachmentId = UUID.randomUUID();

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

    private TaskAttachmentResponse createAttachmentResponse() {
        return TaskAttachmentResponse.builder()
                .id(attachmentId)
                .taskId(taskId)
                .fileName("abc123.pdf")
                .originalFileName("document.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .uploadedBy(userId)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/tasks/{taskId}/attachments - Upload Attachment")
    class UploadAttachmentTests {

        @Test
        @DisplayName("Should return 201 and attachment when upload is successful")
        void shouldUploadAttachment() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "document.pdf",
                    "application/pdf",
                    "test content".getBytes()
            );

            TaskAttachmentResponse response = createAttachmentResponse();
            when(attachmentService.uploadAttachment(eq(taskId), any(), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", taskId)
                            .file(file)
                            .principal(mockAuth))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.originalFileName").value("document.pdf"))
                    .andExpect(jsonPath("$.contentType").value("application/pdf"));

            verify(attachmentService).uploadAttachment(eq(taskId), any(), eq(userId));
        }

        @Test
        @DisplayName("Should return 404 when task not found")
        void shouldReturn404WhenTaskNotFound() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "document.pdf",
                    "application/pdf",
                    "test content".getBytes()
            );

            when(attachmentService.uploadAttachment(eq(taskId), any(), eq(userId)))
                    .thenThrow(new TaskNotFoundException(taskId));

            // When & Then
            mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", taskId)
                            .file(file)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "document.pdf",
                    "application/pdf",
                    "test content".getBytes()
            );

            when(attachmentService.uploadAttachment(eq(taskId), any(), eq(userId)))
                    .thenThrow(AccessDeniedException.notMember());

            // When & Then
            mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", taskId)
                            .file(file)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/{taskId}/attachments - Get Attachments")
    class GetAttachmentsTests {

        @Test
        @DisplayName("Should return 200 and list of attachments")
        void shouldGetAttachments() throws Exception {
            // Given
            List<TaskAttachmentResponse> responses = List.of(createAttachmentResponse());
            when(attachmentService.getAttachments(taskId, userId)).thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/attachments", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].originalFileName").value("document.pdf"))
                    .andExpect(jsonPath("$[0].contentType").value("application/pdf"));

            verify(attachmentService).getAttachments(taskId, userId);
        }

        @Test
        @DisplayName("Should return 200 and empty list when no attachments")
        void shouldReturnEmptyList() throws Exception {
            // Given
            when(attachmentService.getAttachments(taskId, userId)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/attachments", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());

            verify(attachmentService).getAttachments(taskId, userId);
        }

        @Test
        @DisplayName("Should return 404 when task not found")
        void shouldReturn404WhenTaskNotFound() throws Exception {
            // Given
            when(attachmentService.getAttachments(taskId, userId))
                    .thenThrow(new TaskNotFoundException(taskId));

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/attachments", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/{taskId}/attachments/count - Get Attachment Count")
    class GetAttachmentCountTests {

        @Test
        @DisplayName("Should return 200 and attachment count")
        void shouldGetAttachmentCount() throws Exception {
            // Given
            when(attachmentService.getAttachments(taskId, userId)).thenReturn(List.of());
            when(attachmentService.getAttachmentCount(taskId)).thenReturn(5);

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/attachments/count", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(5));

            verify(attachmentService).getAttachmentCount(taskId);
        }

        @Test
        @DisplayName("Should return 0 when no attachments")
        void shouldReturnZeroCount() throws Exception {
            // Given
            when(attachmentService.getAttachments(taskId, userId)).thenReturn(List.of());
            when(attachmentService.getAttachmentCount(taskId)).thenReturn(0);

            // When & Then
            mockMvc.perform(get("/api/tasks/{taskId}/attachments/count", taskId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(0));

            verify(attachmentService).getAttachmentCount(taskId);
        }
    }

    @Nested
    @DisplayName("GET /api/task-attachments/{attachmentId}/download - Download Attachment")
    class DownloadAttachmentTests {

        @Test
        @DisplayName("Should return 200 and file content")
        void shouldDownloadAttachment() throws Exception {
            // Given
            byte[] content = "file content".getBytes();
            Resource resource = new ByteArrayResource(content);

            TaskAttachment attachment = TaskAttachment.builder()
                    .id(attachmentId)
                    .taskId(taskId)
                    .fileName("abc123.pdf")
                    .originalFileName("document.pdf")
                    .filePath("task-attachments/" + taskId + "/abc123.pdf")
                    .fileSize((long) content.length)
                    .contentType("application/pdf")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(attachmentService.downloadAttachment(attachmentId, userId)).thenReturn(resource);
            when(attachmentService.getAttachmentById(attachmentId)).thenReturn(attachment);

            // When & Then
            mockMvc.perform(get("/api/task-attachments/{attachmentId}/download", attachmentId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"document.pdf\""));

            verify(attachmentService).downloadAttachment(attachmentId, userId);
        }

        @Test
        @DisplayName("Should return 404 when attachment not found")
        void shouldReturn404WhenAttachmentNotFound() throws Exception {
            // Given
            when(attachmentService.downloadAttachment(attachmentId, userId))
                    .thenThrow(new TaskAttachmentNotFoundException(attachmentId));

            // When & Then
            mockMvc.perform(get("/api/task-attachments/{attachmentId}/download", attachmentId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/task-attachments/{attachmentId} - Delete Attachment")
    class DeleteAttachmentTests {

        @Test
        @DisplayName("Should return 204 when deletion is successful")
        void shouldDeleteAttachment() throws Exception {
            // Given
            doNothing().when(attachmentService).deleteAttachment(attachmentId, userId);

            // When & Then
            mockMvc.perform(delete("/api/task-attachments/{attachmentId}", attachmentId)
                            .principal(mockAuth))
                    .andExpect(status().isNoContent());

            verify(attachmentService).deleteAttachment(attachmentId, userId);
        }

        @Test
        @DisplayName("Should return 404 when attachment not found")
        void shouldReturn404WhenAttachmentNotFound() throws Exception {
            // Given
            doThrow(new TaskAttachmentNotFoundException(attachmentId))
                    .when(attachmentService).deleteAttachment(attachmentId, userId);

            // When & Then
            mockMvc.perform(delete("/api/task-attachments/{attachmentId}", attachmentId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            doThrow(AccessDeniedException.notMember())
                    .when(attachmentService).deleteAttachment(attachmentId, userId);

            // When & Then
            mockMvc.perform(delete("/api/task-attachments/{attachmentId}", attachmentId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }
}
