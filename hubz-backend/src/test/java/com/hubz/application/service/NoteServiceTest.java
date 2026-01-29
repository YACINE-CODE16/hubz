package com.hubz.application.service;

import com.hubz.application.dto.request.CreateNoteRequest;
import com.hubz.application.dto.request.UpdateNoteRequest;
import com.hubz.application.dto.response.NoteResponse;
import com.hubz.application.port.out.NoteRepositoryPort;
import com.hubz.domain.exception.NoteNotFoundException;
import com.hubz.domain.model.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoteService Unit Tests")
class NoteServiceTest {

    @Mock
    private NoteRepositoryPort noteRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private NoteService noteService;

    private UUID organizationId;
    private UUID userId;
    private UUID noteId;
    private Note testNote;
    private CreateNoteRequest createRequest;
    private UpdateNoteRequest updateRequest;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        noteId = UUID.randomUUID();

        testNote = Note.builder()
                .id(noteId)
                .title("Test Note")
                .content("This is the note content")
                .category("Documentation")
                .organizationId(organizationId)
                .createdById(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = new CreateNoteRequest();
        createRequest.setTitle("Test Note");
        createRequest.setContent("This is the note content");
        createRequest.setCategory("Documentation");

        updateRequest = new UpdateNoteRequest();
        updateRequest.setTitle("Updated Note");
        updateRequest.setContent("Updated content");
        updateRequest.setCategory("Updated Category");
    }

    @Nested
    @DisplayName("Get Notes By Organization Tests")
    class GetByOrganizationTests {

        @Test
        @DisplayName("Should successfully get notes by organization")
        void shouldGetNotesByOrganization() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testNote));

            // When
            List<NoteResponse> notes = noteService.getByOrganization(organizationId, userId);

            // Then
            assertThat(notes).hasSize(1);
            assertThat(notes.get(0).getTitle()).isEqualTo(testNote.getTitle());
            assertThat(notes.get(0).getOrganizationId()).isEqualTo(organizationId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(noteRepository).findByOrganizationId(organizationId);
        }

        @Test
        @DisplayName("Should return empty list when no notes exist")
        void shouldReturnEmptyListWhenNoNotes() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(List.of());

            // When
            List<NoteResponse> notes = noteService.getByOrganization(organizationId, userId);

            // Then
            assertThat(notes).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when user has no access")
        void shouldThrowExceptionWhenNoAccess() {
            // Given
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> noteService.getByOrganization(organizationId, userId))
                    .isInstanceOf(RuntimeException.class);
            verify(noteRepository, never()).findByOrganizationId(any());
        }
    }

    @Nested
    @DisplayName("Get Notes By Organization And Category Tests")
    class GetByOrganizationAndCategoryTests {

        @Test
        @DisplayName("Should successfully get notes by organization and category")
        void shouldGetNotesByOrganizationAndCategory() {
            // Given
            String category = "Documentation";
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteRepository.findByOrganizationIdAndCategory(organizationId, category))
                    .thenReturn(List.of(testNote));

            // When
            List<NoteResponse> notes = noteService.getByOrganizationAndCategory(organizationId, category, userId);

            // Then
            assertThat(notes).hasSize(1);
            assertThat(notes.get(0).getCategory()).isEqualTo(category);
            verify(noteRepository).findByOrganizationIdAndCategory(organizationId, category);
        }

        @Test
        @DisplayName("Should return empty list when no notes match category")
        void shouldReturnEmptyListWhenNoMatchingCategory() {
            // Given
            String category = "NonExistentCategory";
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteRepository.findByOrganizationIdAndCategory(organizationId, category))
                    .thenReturn(List.of());

            // When
            List<NoteResponse> notes = noteService.getByOrganizationAndCategory(organizationId, category, userId);

            // Then
            assertThat(notes).isEmpty();
        }
    }

    @Nested
    @DisplayName("Create Note Tests")
    class CreateTests {

        @Test
        @DisplayName("Should successfully create note")
        void shouldCreateNote() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteRepository.save(any(Note.class))).thenReturn(testNote);

            // When
            NoteResponse response = noteService.create(createRequest, organizationId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo(testNote.getTitle());
            assertThat(response.getContent()).isEqualTo(testNote.getContent());
            assertThat(response.getCategory()).isEqualTo(testNote.getCategory());
            assertThat(response.getOrganizationId()).isEqualTo(organizationId);
            assertThat(response.getCreatedById()).isEqualTo(userId);

            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(noteRepository).save(any(Note.class));
        }

        @Test
        @DisplayName("Should set timestamps when creating note")
        void shouldSetTimestamps() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            when(noteRepository.save(noteCaptor.capture())).thenReturn(testNote);

            // When
            noteService.create(createRequest, organizationId, userId);

            // Then
            Note savedNote = noteCaptor.getValue();
            assertThat(savedNote.getCreatedAt()).isNotNull();
            assertThat(savedNote.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should set all note properties from request")
        void shouldSetAllPropertiesFromRequest() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            when(noteRepository.save(noteCaptor.capture())).thenReturn(testNote);

            // When
            noteService.create(createRequest, organizationId, userId);

            // Then
            Note savedNote = noteCaptor.getValue();
            assertThat(savedNote.getTitle()).isEqualTo(createRequest.getTitle());
            assertThat(savedNote.getContent()).isEqualTo(createRequest.getContent());
            assertThat(savedNote.getCategory()).isEqualTo(createRequest.getCategory());
            assertThat(savedNote.getOrganizationId()).isEqualTo(organizationId);
            assertThat(savedNote.getCreatedById()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should throw exception when user has no access")
        void shouldThrowExceptionWhenNoAccess() {
            // Given
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> noteService.create(createRequest, organizationId, userId))
                    .isInstanceOf(RuntimeException.class);
            verify(noteRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Update Note Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should successfully update note")
        void shouldUpdateNote() {
            // Given
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteRepository.save(any(Note.class))).thenReturn(testNote);

            // When
            NoteResponse response = noteService.update(noteId, updateRequest, userId);

            // Then
            assertThat(response).isNotNull();
            verify(noteRepository).findById(noteId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(noteRepository).save(any(Note.class));
        }

        @Test
        @DisplayName("Should throw exception when note not found")
        void shouldThrowExceptionWhenNoteNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(noteRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> noteService.update(nonExistentId, updateRequest, userId))
                    .isInstanceOf(NoteNotFoundException.class);
            verify(noteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user has no access")
        void shouldThrowExceptionWhenNoAccess() {
            // Given
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> noteService.update(noteId, updateRequest, userId))
                    .isInstanceOf(RuntimeException.class);
            verify(noteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update updatedAt timestamp")
        void shouldUpdateTimestamp() {
            // Given
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            when(noteRepository.save(noteCaptor.capture())).thenReturn(testNote);

            // When
            noteService.update(noteId, updateRequest, userId);

            // Then
            Note updatedNote = noteCaptor.getValue();
            assertThat(updatedNote.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should update all fields from request")
        void shouldUpdateAllFieldsFromRequest() {
            // Given
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            when(noteRepository.save(noteCaptor.capture())).thenReturn(testNote);

            // When
            noteService.update(noteId, updateRequest, userId);

            // Then
            Note updatedNote = noteCaptor.getValue();
            assertThat(updatedNote.getTitle()).isEqualTo(updateRequest.getTitle());
            assertThat(updatedNote.getContent()).isEqualTo(updateRequest.getContent());
            assertThat(updatedNote.getCategory()).isEqualTo(updateRequest.getCategory());
        }
    }

    @Nested
    @DisplayName("Delete Note Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should successfully delete note")
        void shouldDeleteNote() {
            // Given
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            doNothing().when(noteRepository).delete(testNote);

            // When
            noteService.delete(noteId, userId);

            // Then
            verify(noteRepository).findById(noteId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(noteRepository).delete(testNote);
        }

        @Test
        @DisplayName("Should throw exception when note not found")
        void shouldThrowExceptionWhenNoteNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(noteRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> noteService.delete(nonExistentId, userId))
                    .isInstanceOf(NoteNotFoundException.class);
            verify(noteRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when user has no access")
        void shouldThrowExceptionWhenNoAccess() {
            // Given
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> noteService.delete(noteId, userId))
                    .isInstanceOf(RuntimeException.class);
            verify(noteRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should correctly map note to response")
        void shouldCorrectlyMapNoteToResponse() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testNote));

            // When
            List<NoteResponse> notes = noteService.getByOrganization(organizationId, userId);

            // Then
            NoteResponse response = notes.get(0);
            assertThat(response.getId()).isEqualTo(testNote.getId());
            assertThat(response.getTitle()).isEqualTo(testNote.getTitle());
            assertThat(response.getContent()).isEqualTo(testNote.getContent());
            assertThat(response.getCategory()).isEqualTo(testNote.getCategory());
            assertThat(response.getOrganizationId()).isEqualTo(testNote.getOrganizationId());
            assertThat(response.getCreatedById()).isEqualTo(testNote.getCreatedById());
            assertThat(response.getCreatedAt()).isEqualTo(testNote.getCreatedAt());
            assertThat(response.getUpdatedAt()).isEqualTo(testNote.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("Category Tests")
    class CategoryTests {

        @Test
        @DisplayName("Should create note with null category")
        void shouldCreateNoteWithNullCategory() {
            // Given
            CreateNoteRequest requestWithoutCategory = new CreateNoteRequest();
            requestWithoutCategory.setTitle("Note Without Category");
            requestWithoutCategory.setContent("Content");
            requestWithoutCategory.setCategory(null);

            Note noteWithoutCategory = Note.builder()
                    .id(UUID.randomUUID())
                    .title("Note Without Category")
                    .content("Content")
                    .category(null)
                    .organizationId(organizationId)
                    .createdById(userId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteRepository.save(any(Note.class))).thenReturn(noteWithoutCategory);

            // When
            NoteResponse response = noteService.create(requestWithoutCategory, organizationId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getCategory()).isNull();
        }

        @Test
        @DisplayName("Should filter notes by category")
        void shouldFilterNotesByCategory() {
            // Given
            Note docNote = Note.builder()
                    .id(UUID.randomUUID())
                    .title("Documentation Note")
                    .content("Doc content")
                    .category("Documentation")
                    .organizationId(organizationId)
                    .createdById(userId)
                    .build();

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteRepository.findByOrganizationIdAndCategory(organizationId, "Documentation"))
                    .thenReturn(List.of(docNote));

            // When
            List<NoteResponse> notes = noteService.getByOrganizationAndCategory(
                    organizationId, "Documentation", userId);

            // Then
            assertThat(notes).hasSize(1);
            assertThat(notes.get(0).getCategory()).isEqualTo("Documentation");
        }
    }
}
