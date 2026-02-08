package com.hubz.presentation.controller;

import com.hubz.application.dto.response.NoteAttachmentResponse;
import com.hubz.application.port.out.NoteAttachmentRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.NoteAttachmentService;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.model.NoteAttachment;
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
        value = NoteAttachmentController.class,
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
@DisplayName("NoteAttachmentController Unit Tests")
class NoteAttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NoteAttachmentService attachmentService;

    @MockBean
    private NoteAttachmentRepositoryPort attachmentRepository;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private UUID noteId;
    private UUID attachmentId;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        noteId = UUID.randomUUID();
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

    private NoteAttachmentResponse createAttachmentResponse() {
        return NoteAttachmentResponse.builder()
                .id(attachmentId)
                .noteId(noteId)
                .fileName("test-file.pdf")
                .originalFileName("test-file.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .uploadedBy(userId)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/notes/{noteId}/attachments - Upload Attachment")
    class UploadTests {

        @Test
        @DisplayName("Should return 201 when upload is successful")
        void shouldUploadAttachment() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test-file.pdf",
                    "application/pdf",
                    "Test file content".getBytes()
            );

            NoteAttachmentResponse response = createAttachmentResponse();
            when(attachmentService.uploadAttachment(eq(noteId), any(), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(multipart("/api/notes/{noteId}/attachments", noteId)
                            .file(file)
                            .principal(mockAuth))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.fileName").value("test-file.pdf"))
                    .andExpect(jsonPath("$.contentType").value("application/pdf"));

            verify(attachmentService).uploadAttachment(eq(noteId), any(), eq(userId));
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test-file.pdf",
                    "application/pdf",
                    "Test file content".getBytes()
            );

            when(attachmentService.uploadAttachment(eq(noteId), any(), eq(userId)))
                    .thenThrow(new AccessDeniedException("Not authorized"));

            // When & Then
            mockMvc.perform(multipart("/api/notes/{noteId}/attachments", noteId)
                            .file(file)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/notes/{noteId}/attachments - Get Attachments")
    class GetAttachmentsTests {

        @Test
        @DisplayName("Should return 200 and list of attachments")
        void shouldGetAttachments() throws Exception {
            // Given
            List<NoteAttachmentResponse> responses = List.of(createAttachmentResponse());
            when(attachmentService.getAttachments(noteId, userId)).thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/notes/{noteId}/attachments", noteId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].fileName").value("test-file.pdf"));

            verify(attachmentService).getAttachments(noteId, userId);
        }

        @Test
        @DisplayName("Should return 200 and empty list when no attachments")
        void shouldReturnEmptyList() throws Exception {
            // Given
            when(attachmentService.getAttachments(noteId, userId)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/notes/{noteId}/attachments", noteId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());

            verify(attachmentService).getAttachments(noteId, userId);
        }
    }

    @Nested
    @DisplayName("GET /api/attachments/{attachmentId}/download - Download Attachment")
    class DownloadTests {

        @Test
        @DisplayName("Should return 200 and file resource when download is successful")
        void shouldDownloadAttachment() throws Exception {
            // Given
            byte[] fileContent = "Test file content".getBytes();
            Resource resource = new ByteArrayResource(fileContent);

            NoteAttachment attachment = NoteAttachment.builder()
                    .id(attachmentId)
                    .noteId(noteId)
                    .originalFileName("test-file.pdf")
                    .contentType("application/pdf")
                    .build();

            when(attachmentService.downloadAttachment(attachmentId, userId)).thenReturn(resource);
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

            // When & Then
            mockMvc.perform(get("/api/attachments/{attachmentId}/download", attachmentId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"test-file.pdf\""))
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            when(attachmentService.downloadAttachment(attachmentId, userId))
                    .thenThrow(new AccessDeniedException("Not authorized"));

            // When & Then
            mockMvc.perform(get("/api/attachments/{attachmentId}/download", attachmentId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/attachments/{attachmentId} - Delete Attachment")
    class DeleteTests {

        @Test
        @DisplayName("Should return 204 when deletion is successful")
        void shouldDeleteAttachment() throws Exception {
            // Given
            doNothing().when(attachmentService).deleteAttachment(attachmentId, userId);

            // When & Then
            mockMvc.perform(delete("/api/attachments/{attachmentId}", attachmentId)
                            .principal(mockAuth))
                    .andExpect(status().isNoContent());

            verify(attachmentService).deleteAttachment(attachmentId, userId);
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            doThrow(new AccessDeniedException("Not authorized"))
                    .when(attachmentService).deleteAttachment(attachmentId, userId);

            // When & Then
            mockMvc.perform(delete("/api/attachments/{attachmentId}", attachmentId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }
}
