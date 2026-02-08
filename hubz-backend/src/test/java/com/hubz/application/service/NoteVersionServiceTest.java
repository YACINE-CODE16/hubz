package com.hubz.application.service;

import com.hubz.application.dto.response.NoteVersionResponse;
import com.hubz.application.port.out.NoteRepositoryPort;
import com.hubz.application.port.out.NoteVersionRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.NoteNotFoundException;
import com.hubz.domain.exception.NoteVersionNotFoundException;
import com.hubz.domain.model.Note;
import com.hubz.domain.model.NoteVersion;
import com.hubz.domain.model.User;
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
@DisplayName("NoteVersionService Unit Tests")
class NoteVersionServiceTest {

    @Mock
    private NoteVersionRepositoryPort noteVersionRepository;

    @Mock
    private NoteRepositoryPort noteRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private NoteVersionService noteVersionService;

    private UUID organizationId;
    private UUID userId;
    private UUID noteId;
    private UUID versionId;
    private Note testNote;
    private NoteVersion testVersion;
    private User testUser;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        noteId = UUID.randomUUID();
        versionId = UUID.randomUUID();

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

        testVersion = NoteVersion.builder()
                .id(versionId)
                .noteId(noteId)
                .versionNumber(1)
                .title("Test Note")
                .content("This is the note content")
                .createdById(userId)
                .createdAt(LocalDateTime.now())
                .build();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Nested
    @DisplayName("Create Version Tests")
    class CreateVersionTests {

        @Test
        @DisplayName("Should create first version with version number 1")
        void shouldCreateFirstVersionWithVersionNumber1() {
            // Given
            when(noteVersionRepository.findMaxVersionNumberByNoteId(noteId))
                    .thenReturn(Optional.empty());
            when(noteVersionRepository.save(any(NoteVersion.class)))
                    .thenReturn(testVersion);

            // When
            NoteVersion result = noteVersionService.createVersion(testNote, userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getVersionNumber()).isEqualTo(1);

            ArgumentCaptor<NoteVersion> captor = ArgumentCaptor.forClass(NoteVersion.class);
            verify(noteVersionRepository).save(captor.capture());
            NoteVersion savedVersion = captor.getValue();
            assertThat(savedVersion.getVersionNumber()).isEqualTo(1);
            assertThat(savedVersion.getNoteId()).isEqualTo(noteId);
            assertThat(savedVersion.getTitle()).isEqualTo(testNote.getTitle());
            assertThat(savedVersion.getContent()).isEqualTo(testNote.getContent());
            assertThat(savedVersion.getCreatedById()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should increment version number for subsequent versions")
        void shouldIncrementVersionNumber() {
            // Given
            when(noteVersionRepository.findMaxVersionNumberByNoteId(noteId))
                    .thenReturn(Optional.of(3));
            NoteVersion version4 = NoteVersion.builder()
                    .id(UUID.randomUUID())
                    .noteId(noteId)
                    .versionNumber(4)
                    .title(testNote.getTitle())
                    .content(testNote.getContent())
                    .createdById(userId)
                    .createdAt(LocalDateTime.now())
                    .build();
            when(noteVersionRepository.save(any(NoteVersion.class)))
                    .thenReturn(version4);

            // When
            NoteVersion result = noteVersionService.createVersion(testNote, userId);

            // Then
            assertThat(result.getVersionNumber()).isEqualTo(4);

            ArgumentCaptor<NoteVersion> captor = ArgumentCaptor.forClass(NoteVersion.class);
            verify(noteVersionRepository).save(captor.capture());
            assertThat(captor.getValue().getVersionNumber()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should set createdAt timestamp")
        void shouldSetCreatedAtTimestamp() {
            // Given
            when(noteVersionRepository.findMaxVersionNumberByNoteId(noteId))
                    .thenReturn(Optional.empty());
            when(noteVersionRepository.save(any(NoteVersion.class)))
                    .thenReturn(testVersion);

            // When
            noteVersionService.createVersion(testNote, userId);

            // Then
            ArgumentCaptor<NoteVersion> captor = ArgumentCaptor.forClass(NoteVersion.class);
            verify(noteVersionRepository).save(captor.capture());
            assertThat(captor.getValue().getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Get Versions By Note ID Tests")
    class GetVersionsByNoteIdTests {

        @Test
        @DisplayName("Should return versions ordered by version number descending")
        void shouldReturnVersionsOrderedByVersionNumberDesc() {
            // Given
            NoteVersion version1 = NoteVersion.builder()
                    .id(UUID.randomUUID())
                    .noteId(noteId)
                    .versionNumber(1)
                    .title("Version 1")
                    .content("Content 1")
                    .createdById(userId)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build();
            NoteVersion version2 = NoteVersion.builder()
                    .id(UUID.randomUUID())
                    .noteId(noteId)
                    .versionNumber(2)
                    .title("Version 2")
                    .content("Content 2")
                    .createdById(userId)
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();

            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteVersionRepository.findByNoteIdOrderByVersionNumberDesc(noteId))
                    .thenReturn(List.of(version2, version1));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            List<NoteVersionResponse> versions = noteVersionService.getVersionsByNoteId(noteId, userId);

            // Then
            assertThat(versions).hasSize(2);
            assertThat(versions.get(0).getVersionNumber()).isEqualTo(2);
            assertThat(versions.get(1).getVersionNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return empty list when no versions exist")
        void shouldReturnEmptyListWhenNoVersions() {
            // Given
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteVersionRepository.findByNoteIdOrderByVersionNumberDesc(noteId))
                    .thenReturn(List.of());

            // When
            List<NoteVersionResponse> versions = noteVersionService.getVersionsByNoteId(noteId, userId);

            // Then
            assertThat(versions).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when note not found")
        void shouldThrowExceptionWhenNoteNotFound() {
            // Given
            UUID nonExistentNoteId = UUID.randomUUID();
            when(noteRepository.findById(nonExistentNoteId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> noteVersionService.getVersionsByNoteId(nonExistentNoteId, userId))
                    .isInstanceOf(NoteNotFoundException.class);
            verify(noteVersionRepository, never()).findByNoteIdOrderByVersionNumberDesc(any());
        }

        @Test
        @DisplayName("Should throw exception when user has no access")
        void shouldThrowExceptionWhenNoAccess() {
            // Given
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> noteVersionService.getVersionsByNoteId(noteId, userId))
                    .isInstanceOf(RuntimeException.class);
            verify(noteVersionRepository, never()).findByNoteIdOrderByVersionNumberDesc(any());
        }

        @Test
        @DisplayName("Should include createdByName in response")
        void shouldIncludeCreatedByNameInResponse() {
            // Given
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteVersionRepository.findByNoteIdOrderByVersionNumberDesc(noteId))
                    .thenReturn(List.of(testVersion));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            List<NoteVersionResponse> versions = noteVersionService.getVersionsByNoteId(noteId, userId);

            // Then
            assertThat(versions).hasSize(1);
            assertThat(versions.get(0).getCreatedByName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should show 'Unknown User' when user not found")
        void shouldShowUnknownUserWhenUserNotFound() {
            // Given
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteVersionRepository.findByNoteIdOrderByVersionNumberDesc(noteId))
                    .thenReturn(List.of(testVersion));
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When
            List<NoteVersionResponse> versions = noteVersionService.getVersionsByNoteId(noteId, userId);

            // Then
            assertThat(versions).hasSize(1);
            assertThat(versions.get(0).getCreatedByName()).isEqualTo("Unknown User");
        }
    }

    @Nested
    @DisplayName("Get Version By ID Tests")
    class GetVersionByIdTests {

        @Test
        @DisplayName("Should return version by ID")
        void shouldReturnVersionById() {
            // Given
            when(noteVersionRepository.findById(versionId)).thenReturn(Optional.of(testVersion));
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            NoteVersionResponse response = noteVersionService.getVersionById(versionId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(versionId);
            assertThat(response.getVersionNumber()).isEqualTo(1);
            assertThat(response.getTitle()).isEqualTo(testVersion.getTitle());
        }

        @Test
        @DisplayName("Should throw exception when version not found")
        void shouldThrowExceptionWhenVersionNotFound() {
            // Given
            UUID nonExistentVersionId = UUID.randomUUID();
            when(noteVersionRepository.findById(nonExistentVersionId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> noteVersionService.getVersionById(nonExistentVersionId, userId))
                    .isInstanceOf(NoteVersionNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when note not found")
        void shouldThrowExceptionWhenNoteNotFound() {
            // Given
            when(noteVersionRepository.findById(versionId)).thenReturn(Optional.of(testVersion));
            when(noteRepository.findById(noteId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> noteVersionService.getVersionById(versionId, userId))
                    .isInstanceOf(NoteNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when user has no access")
        void shouldThrowExceptionWhenNoAccess() {
            // Given
            when(noteVersionRepository.findById(versionId)).thenReturn(Optional.of(testVersion));
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> noteVersionService.getVersionById(versionId, userId))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Restore Version Tests")
    class RestoreVersionTests {

        @Test
        @DisplayName("Should restore note to specified version")
        void shouldRestoreNoteToSpecifiedVersion() {
            // Given
            NoteVersion oldVersion = NoteVersion.builder()
                    .id(versionId)
                    .noteId(noteId)
                    .versionNumber(1)
                    .title("Old Title")
                    .content("Old Content")
                    .createdById(userId)
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();

            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteVersionRepository.findById(versionId)).thenReturn(Optional.of(oldVersion));
            when(noteVersionRepository.findMaxVersionNumberByNoteId(noteId))
                    .thenReturn(Optional.of(2))
                    .thenReturn(Optional.of(3));
            when(noteVersionRepository.save(any(NoteVersion.class)))
                    .thenAnswer(invocation -> {
                        NoteVersion v = invocation.getArgument(0);
                        v.setId(UUID.randomUUID());
                        return v;
                    });
            when(noteRepository.save(any(Note.class))).thenReturn(testNote);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            NoteVersionResponse response = noteVersionService.restoreVersion(noteId, versionId, userId);

            // Then
            assertThat(response).isNotNull();
            verify(noteRepository).save(any(Note.class));
            // Should create 2 versions: one for current state, one for restored state
            verify(noteVersionRepository, times(2)).save(any(NoteVersion.class));
        }

        @Test
        @DisplayName("Should update note with restored content")
        void shouldUpdateNoteWithRestoredContent() {
            // Given
            NoteVersion oldVersion = NoteVersion.builder()
                    .id(versionId)
                    .noteId(noteId)
                    .versionNumber(1)
                    .title("Old Title")
                    .content("Old Content")
                    .createdById(userId)
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();

            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteVersionRepository.findById(versionId)).thenReturn(Optional.of(oldVersion));
            when(noteVersionRepository.findMaxVersionNumberByNoteId(noteId))
                    .thenReturn(Optional.of(2))
                    .thenReturn(Optional.of(3));
            when(noteVersionRepository.save(any(NoteVersion.class)))
                    .thenAnswer(invocation -> {
                        NoteVersion v = invocation.getArgument(0);
                        v.setId(UUID.randomUUID());
                        return v;
                    });

            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            when(noteRepository.save(noteCaptor.capture())).thenReturn(testNote);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            noteVersionService.restoreVersion(noteId, versionId, userId);

            // Then
            Note savedNote = noteCaptor.getValue();
            assertThat(savedNote.getTitle()).isEqualTo("Old Title");
            assertThat(savedNote.getContent()).isEqualTo("Old Content");
        }

        @Test
        @DisplayName("Should throw exception when note not found")
        void shouldThrowExceptionWhenNoteNotFound() {
            // Given
            UUID nonExistentNoteId = UUID.randomUUID();
            when(noteRepository.findById(nonExistentNoteId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> noteVersionService.restoreVersion(nonExistentNoteId, versionId, userId))
                    .isInstanceOf(NoteNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when version not found")
        void shouldThrowExceptionWhenVersionNotFound() {
            // Given
            UUID nonExistentVersionId = UUID.randomUUID();
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteVersionRepository.findById(nonExistentVersionId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> noteVersionService.restoreVersion(noteId, nonExistentVersionId, userId))
                    .isInstanceOf(NoteVersionNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when version does not belong to note")
        void shouldThrowExceptionWhenVersionDoesNotBelongToNote() {
            // Given
            UUID otherNoteId = UUID.randomUUID();
            NoteVersion versionForOtherNote = NoteVersion.builder()
                    .id(versionId)
                    .noteId(otherNoteId)
                    .versionNumber(1)
                    .title("Other Note")
                    .content("Content")
                    .createdById(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteVersionRepository.findById(versionId)).thenReturn(Optional.of(versionForOtherNote));

            // When & Then
            assertThatThrownBy(() -> noteVersionService.restoreVersion(noteId, versionId, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Version does not belong to this note");
        }

        @Test
        @DisplayName("Should throw exception when user has no access")
        void shouldThrowExceptionWhenNoAccess() {
            // Given
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> noteVersionService.restoreVersion(noteId, versionId, userId))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Delete Versions By Note ID Tests")
    class DeleteVersionsByNoteIdTests {

        @Test
        @DisplayName("Should delete all versions for a note")
        void shouldDeleteAllVersionsForNote() {
            // Given
            doNothing().when(noteVersionRepository).deleteByNoteId(noteId);

            // When
            noteVersionService.deleteVersionsByNoteId(noteId);

            // Then
            verify(noteVersionRepository).deleteByNoteId(noteId);
        }
    }

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should correctly map version to response")
        void shouldCorrectlyMapVersionToResponse() {
            // Given
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(noteVersionRepository.findByNoteIdOrderByVersionNumberDesc(noteId))
                    .thenReturn(List.of(testVersion));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            List<NoteVersionResponse> versions = noteVersionService.getVersionsByNoteId(noteId, userId);

            // Then
            NoteVersionResponse response = versions.get(0);
            assertThat(response.getId()).isEqualTo(testVersion.getId());
            assertThat(response.getNoteId()).isEqualTo(testVersion.getNoteId());
            assertThat(response.getVersionNumber()).isEqualTo(testVersion.getVersionNumber());
            assertThat(response.getTitle()).isEqualTo(testVersion.getTitle());
            assertThat(response.getContent()).isEqualTo(testVersion.getContent());
            assertThat(response.getCreatedById()).isEqualTo(testVersion.getCreatedById());
            assertThat(response.getCreatedAt()).isEqualTo(testVersion.getCreatedAt());
            assertThat(response.getCreatedByName()).isEqualTo("John Doe");
        }
    }
}
