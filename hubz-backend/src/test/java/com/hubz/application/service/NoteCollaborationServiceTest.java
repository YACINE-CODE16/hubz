package com.hubz.application.service;

import com.hubz.application.dto.request.NoteCursorRequest;
import com.hubz.application.dto.request.NoteEditRequest;
import com.hubz.application.dto.response.*;
import com.hubz.application.port.out.NoteRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.NoteNotFoundException;
import com.hubz.domain.model.Note;
import com.hubz.domain.model.NoteEditOperation;
import com.hubz.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteCollaborationServiceTest {

    @Mock
    private NoteRepositoryPort noteRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private NoteCollaborationService collaborationService;

    private UUID noteId;
    private UUID userId;
    private UUID organizationId;
    private String userEmail;
    private User testUser;
    private Note testNote;

    @BeforeEach
    void setUp() {
        noteId = UUID.randomUUID();
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        userEmail = "test@example.com";

        testUser = User.builder()
                .id(userId)
                .email(userEmail)
                .firstName("Test")
                .lastName("User")
                .profilePhotoUrl(null)
                .build();

        testNote = Note.builder()
                .id(noteId)
                .title("Test Note")
                .content("Test content")
                .organizationId(organizationId)
                .createdById(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should allow user to join note session")
    void shouldAllowUserToJoinNoteSession() {
        // Given
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

        // When
        NoteSessionResponse session = collaborationService.joinNote(noteId, userEmail);

        // Then
        assertThat(session).isNotNull();
        assertThat(session.getNoteId()).isEqualTo(noteId);
        assertThat(session.getCurrentTitle()).isEqualTo("Test Note");
        assertThat(session.getCurrentContent()).isEqualTo("Test content");
        assertThat(session.getCollaborators()).hasSize(1);
        assertThat(session.getCollaborators().get(0).getEmail()).isEqualTo(userEmail);
        assertThat(session.getVersion()).isEqualTo(1L);

        verify(userRepository).findByEmail(userEmail);
        verify(noteRepository).findById(noteId);
        verify(authorizationService).checkOrganizationAccess(organizationId, userId);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> collaborationService.joinNote(noteId, userEmail))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("Should throw exception when note not found")
    void shouldThrowExceptionWhenNoteNotFound() {
        // Given
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(noteRepository.findById(noteId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> collaborationService.joinNote(noteId, userEmail))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    @DisplayName("Should allow user to leave note session")
    void shouldAllowUserToLeaveNoteSession() {
        // Given - First join
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

        collaborationService.joinNote(noteId, userEmail);

        // When - Leave
        NoteCollaborationEventResponse event = collaborationService.leaveNote(noteId, userEmail);

        // Then
        assertThat(event).isNotNull();
        assertThat(event.getEventType()).isEqualTo(NoteCollaborationEventResponse.EventType.USER_LEFT);
        assertThat(event.getNoteId()).isEqualTo(noteId);
        assertThat(event.getTotalCollaborators()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should process edit and update version")
    void shouldProcessEditAndUpdateVersion() {
        // Given - First join
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

        collaborationService.joinNote(noteId, userEmail);

        NoteEditRequest editRequest = NoteEditRequest.builder()
                .noteId(noteId)
                .type(NoteEditOperation.EditType.CONTENT_UPDATE)
                .content("Updated content")
                .baseVersion(1L)
                .build();

        // When
        NoteEditResponse response = collaborationService.processEdit(editRequest, userEmail);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getNoteId()).isEqualTo(noteId);
        assertThat(response.getContent()).isEqualTo("Updated content");
        assertThat(response.getVersion()).isEqualTo(2L);
        assertThat(response.isHasConflict()).isFalse();
    }

    @Test
    @DisplayName("Should detect conflict when base version is older")
    void shouldDetectConflictWhenBaseVersionIsOlder() {
        // Given - First join
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

        collaborationService.joinNote(noteId, userEmail);

        // First edit - version becomes 2
        NoteEditRequest firstEdit = NoteEditRequest.builder()
                .noteId(noteId)
                .type(NoteEditOperation.EditType.CONTENT_UPDATE)
                .content("First update")
                .baseVersion(1L)
                .build();
        collaborationService.processEdit(firstEdit, userEmail);

        // Second edit with old base version
        NoteEditRequest conflictingEdit = NoteEditRequest.builder()
                .noteId(noteId)
                .type(NoteEditOperation.EditType.CONTENT_UPDATE)
                .content("Conflicting update")
                .baseVersion(1L) // Old version
                .build();

        // When
        NoteEditResponse response = collaborationService.processEdit(conflictingEdit, userEmail);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isHasConflict()).isTrue();
        assertThat(response.getConflictMessage()).isNotNull();
    }

    @Test
    @DisplayName("Should update cursor position")
    void shouldUpdateCursorPosition() {
        // Given - First join
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

        collaborationService.joinNote(noteId, userEmail);

        NoteCursorRequest cursorRequest = NoteCursorRequest.builder()
                .noteId(noteId)
                .position(42)
                .selectionStart(40)
                .selectionEnd(50)
                .build();

        // When
        NoteCursorResponse cursor = collaborationService.updateCursor(cursorRequest, userEmail);

        // Then
        assertThat(cursor).isNotNull();
        assertThat(cursor.getUserId()).isEqualTo(userId);
        assertThat(cursor.getPosition()).isEqualTo(42);
        assertThat(cursor.getSelectionStart()).isEqualTo(40);
        assertThat(cursor.getSelectionEnd()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should return null for cursor update when no session exists")
    void shouldReturnNullForCursorUpdateWhenNoSession() {
        // Given
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));

        NoteCursorRequest cursorRequest = NoteCursorRequest.builder()
                .noteId(noteId)
                .position(42)
                .build();

        // When
        NoteCursorResponse cursor = collaborationService.updateCursor(cursorRequest, userEmail);

        // Then
        assertThat(cursor).isNull();
    }

    @Test
    @DisplayName("Should create typing event")
    void shouldCreateTypingEvent() {
        // Given - First join
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

        collaborationService.joinNote(noteId, userEmail);

        // When - User typing
        NoteCollaborationEventResponse typingEvent = collaborationService.createTypingEvent(noteId, userEmail, true);

        // Then
        assertThat(typingEvent).isNotNull();
        assertThat(typingEvent.getEventType()).isEqualTo(NoteCollaborationEventResponse.EventType.USER_TYPING);
        assertThat(typingEvent.getCollaborator().getEmail()).isEqualTo(userEmail);

        // When - User stopped typing
        NoteCollaborationEventResponse stoppedEvent = collaborationService.createTypingEvent(noteId, userEmail, false);

        // Then
        assertThat(stoppedEvent).isNotNull();
        assertThat(stoppedEvent.getEventType()).isEqualTo(NoteCollaborationEventResponse.EventType.USER_STOPPED_TYPING);
    }

    @Test
    @DisplayName("Should get collaborator count")
    void shouldGetCollaboratorCount() {
        // Given - No session yet
        int initialCount = collaborationService.getCollaboratorCount(noteId);
        assertThat(initialCount).isEqualTo(0);

        // Join session
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

        collaborationService.joinNote(noteId, userEmail);

        // When
        int count = collaborationService.getCollaboratorCount(noteId);

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should get collaborators list")
    void shouldGetCollaboratorsList() {
        // Given
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

        collaborationService.joinNote(noteId, userEmail);

        // When
        var collaborators = collaborationService.getCollaborators(noteId);

        // Then
        assertThat(collaborators).hasSize(1);
        assertThat(collaborators.get(0).getEmail()).isEqualTo(userEmail);
        assertThat(collaborators.get(0).getDisplayName()).isEqualTo("Test User");
        assertThat(collaborators.get(0).getInitials()).isEqualTo("TU");
    }

    @Test
    @DisplayName("Should handle user disconnect and cleanup sessions")
    void shouldHandleUserDisconnectAndCleanupSessions() {
        // Given - Join two notes
        UUID noteId2 = UUID.randomUUID();
        Note testNote2 = Note.builder()
                .id(noteId2)
                .title("Test Note 2")
                .content("Content 2")
                .organizationId(organizationId)
                .createdById(userId)
                .build();

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
        when(noteRepository.findById(noteId2)).thenReturn(Optional.of(testNote2));
        doNothing().when(authorizationService).checkOrganizationAccess(any(), any());

        collaborationService.joinNote(noteId, userEmail);
        collaborationService.joinNote(noteId2, userEmail);

        assertThat(collaborationService.getCollaboratorCount(noteId)).isEqualTo(1);
        assertThat(collaborationService.getCollaboratorCount(noteId2)).isEqualTo(1);

        // When - User disconnects
        collaborationService.handleUserDisconnect(userEmail);

        // Then - Both sessions should be cleaned up
        assertThat(collaborationService.getCollaboratorCount(noteId)).isEqualTo(0);
        assertThat(collaborationService.getCollaboratorCount(noteId2)).isEqualTo(0);
    }

    @Test
    @DisplayName("Should assign different colors to collaborators")
    void shouldAssignDifferentColorsToCollaborators() {
        // Given
        String user1Email = "user1@example.com";
        String user2Email = "user2@example.com";

        User user1 = User.builder()
                .id(UUID.randomUUID())
                .email(user1Email)
                .firstName("User")
                .lastName("One")
                .build();

        User user2 = User.builder()
                .id(UUID.randomUUID())
                .email(user2Email)
                .firstName("User")
                .lastName("Two")
                .build();

        when(userRepository.findByEmail(user1Email)).thenReturn(Optional.of(user1));
        when(userRepository.findByEmail(user2Email)).thenReturn(Optional.of(user2));
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
        doNothing().when(authorizationService).checkOrganizationAccess(any(), any());

        // When
        collaborationService.joinNote(noteId, user1Email);
        NoteSessionResponse session2 = collaborationService.joinNote(noteId, user2Email);

        // Then
        assertThat(session2.getCollaborators()).hasSize(2);
        String color1 = session2.getCollaborators().stream()
                .filter(c -> c.getEmail().equals(user1Email))
                .findFirst()
                .map(NoteCollaboratorResponse::getColor)
                .orElse(null);
        String color2 = session2.getCollaborators().stream()
                .filter(c -> c.getEmail().equals(user2Email))
                .findFirst()
                .map(NoteCollaboratorResponse::getColor)
                .orElse(null);

        assertThat(color1).isNotNull();
        assertThat(color2).isNotNull();
        assertThat(color1).isNotEqualTo(color2);
    }

    @Test
    @DisplayName("Should process title update")
    void shouldProcessTitleUpdate() {
        // Given - Join session
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

        collaborationService.joinNote(noteId, userEmail);

        NoteEditRequest editRequest = NoteEditRequest.builder()
                .noteId(noteId)
                .type(NoteEditOperation.EditType.TITLE_UPDATE)
                .title("Updated Title")
                .baseVersion(1L)
                .build();

        // When
        NoteEditResponse response = collaborationService.processEdit(editRequest, userEmail);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Updated Title");
        assertThat(response.getType()).isEqualTo(NoteEditOperation.EditType.TITLE_UPDATE);

        // Verify session is updated
        NoteSessionResponse session = collaborationService.getSession(noteId);
        assertThat(session.getCurrentTitle()).isEqualTo("Updated Title");
    }

    @Test
    @DisplayName("Should process full update")
    void shouldProcessFullUpdate() {
        // Given - Join session
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

        collaborationService.joinNote(noteId, userEmail);

        NoteEditRequest editRequest = NoteEditRequest.builder()
                .noteId(noteId)
                .type(NoteEditOperation.EditType.FULL_UPDATE)
                .title("New Title")
                .content("New Content")
                .baseVersion(1L)
                .build();

        // When
        NoteEditResponse response = collaborationService.processEdit(editRequest, userEmail);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("New Title");
        assertThat(response.getContent()).isEqualTo("New Content");

        // Verify session is updated
        NoteSessionResponse session = collaborationService.getSession(noteId);
        assertThat(session.getCurrentTitle()).isEqualTo("New Title");
        assertThat(session.getCurrentContent()).isEqualTo("New Content");
    }
}
