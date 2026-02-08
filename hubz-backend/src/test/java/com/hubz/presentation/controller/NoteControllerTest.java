package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateNoteRequest;
import com.hubz.application.dto.request.UpdateNoteRequest;
import com.hubz.application.dto.response.NoteResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.NoteService;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.NoteNotFoundException;
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
        value = NoteController.class,
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
@DisplayName("NoteController Unit Tests")
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NoteService noteService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private UUID orgId;
    private UUID noteId;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        noteId = UUID.randomUUID();

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

    private NoteResponse createNoteResponse() {
        return NoteResponse.builder()
                .id(noteId)
                .title("Test Note")
                .content("Test content")
                .category("General")
                .organizationId(orgId)
                .createdById(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/organizations/{orgId}/notes - Get Notes By Organization")
    class GetByOrganizationTests {

        @Test
        @DisplayName("Should return 200 and list of notes")
        void shouldGetNotesByOrganization() throws Exception {
            // Given
            List<NoteResponse> responses = List.of(createNoteResponse());
            when(noteService.getByOrganization(orgId, userId)).thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/notes", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Test Note"))
                    .andExpect(jsonPath("$[0].content").value("Test content"));

            verify(noteService).getByOrganization(orgId, userId);
        }

        @Test
        @DisplayName("Should return 200 and filtered notes when category is provided")
        void shouldGetNotesByOrganizationAndCategory() throws Exception {
            // Given
            List<NoteResponse> responses = List.of(createNoteResponse());
            when(noteService.getByOrganizationAndCategory(orgId, "General", userId)).thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/notes", orgId)
                            .param("category", "General")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].category").value("General"));

            verify(noteService).getByOrganizationAndCategory(orgId, "General", userId);
        }

        @Test
        @DisplayName("Should return 200 and empty list when no notes")
        void shouldReturnEmptyList() throws Exception {
            // Given
            when(noteService.getByOrganization(orgId, userId)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/notes", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());

            verify(noteService).getByOrganization(orgId, userId);
        }
    }

    @Nested
    @DisplayName("POST /api/organizations/{orgId}/notes - Create Note")
    class CreateTests {

        @Test
        @DisplayName("Should return 201 and note when creation is successful")
        void shouldCreateNote() throws Exception {
            // Given
            NoteResponse response = createNoteResponse();
            when(noteService.create(any(CreateNoteRequest.class), eq(orgId), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/notes", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Test Note",
                                        "content": "Test content",
                                        "category": "General"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Test Note"))
                    .andExpect(jsonPath("$.content").value("Test content"));

            verify(noteService).create(any(CreateNoteRequest.class), eq(orgId), eq(userId));
        }

        @Test
        @DisplayName("Should return 400 when title is blank")
        void shouldReturn400WhenTitleBlank() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/notes", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "",
                                        "content": "Test content"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(noteService, never()).create(any(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when content is blank")
        void shouldReturn400WhenContentBlank() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/notes", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Test Note",
                                        "content": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(noteService, never()).create(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("PUT /api/notes/{id} - Update Note")
    class UpdateTests {

        @Test
        @DisplayName("Should return 200 and updated note when successful")
        void shouldUpdateNote() throws Exception {
            // Given
            NoteResponse response = NoteResponse.builder()
                    .id(noteId)
                    .title("Updated Note")
                    .content("Updated content")
                    .category("Updated")
                    .organizationId(orgId)
                    .createdById(userId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(noteService.update(eq(noteId), any(UpdateNoteRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(put("/api/notes/{id}", noteId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Updated Note",
                                        "content": "Updated content",
                                        "category": "Updated"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Note"))
                    .andExpect(jsonPath("$.content").value("Updated content"));

            verify(noteService).update(eq(noteId), any(UpdateNoteRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 404 when note not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            when(noteService.update(eq(noteId), any(UpdateNoteRequest.class), eq(userId)))
                    .thenThrow(new NoteNotFoundException(noteId));

            // When & Then
            mockMvc.perform(put("/api/notes/{id}", noteId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Updated Note",
                                        "content": "Updated content"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            when(noteService.update(eq(noteId), any(UpdateNoteRequest.class), eq(userId)))
                    .thenThrow(new AccessDeniedException("Not authorized"));

            // When & Then
            mockMvc.perform(put("/api/notes/{id}", noteId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Updated Note",
                                        "content": "Updated content"
                                    }
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/notes/{id} - Delete Note")
    class DeleteTests {

        @Test
        @DisplayName("Should return 204 when deletion is successful")
        void shouldDeleteNote() throws Exception {
            // Given
            doNothing().when(noteService).delete(noteId, userId);

            // When & Then
            mockMvc.perform(delete("/api/notes/{id}", noteId)
                            .principal(mockAuth))
                    .andExpect(status().isNoContent());

            verify(noteService).delete(noteId, userId);
        }

        @Test
        @DisplayName("Should return 404 when note not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            doThrow(new NoteNotFoundException(noteId))
                    .when(noteService).delete(noteId, userId);

            // When & Then
            mockMvc.perform(delete("/api/notes/{id}", noteId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            doThrow(new AccessDeniedException("Not authorized"))
                    .when(noteService).delete(noteId, userId);

            // When & Then
            mockMvc.perform(delete("/api/notes/{id}", noteId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }
}
