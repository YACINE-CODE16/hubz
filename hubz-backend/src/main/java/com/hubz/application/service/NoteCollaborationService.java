package com.hubz.application.service;

import com.hubz.application.dto.request.NoteCursorRequest;
import com.hubz.application.dto.request.NoteEditRequest;
import com.hubz.application.dto.response.*;
import com.hubz.application.port.out.NoteRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.NoteNotFoundException;
import com.hubz.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing real-time note collaboration sessions.
 * Handles user joining/leaving, cursor positions, and edit broadcasting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoteCollaborationService {

    private final NoteRepositoryPort noteRepository;
    private final UserRepositoryPort userRepository;
    private final AuthorizationService authorizationService;

    // In-memory storage for active sessions (could be replaced with Redis for multi-instance deployment)
    private final Map<UUID, NoteSession> activeSessions = new ConcurrentHashMap<>();

    // Predefined colors for collaborators
    private static final List<String> COLLABORATOR_COLORS = List.of(
            "#3B82F6", // Blue
            "#10B981", // Green
            "#F59E0B", // Amber
            "#EF4444", // Red
            "#8B5CF6", // Purple
            "#EC4899", // Pink
            "#06B6D4", // Cyan
            "#F97316"  // Orange
    );

    /**
     * User joins a note editing session.
     */
    public NoteSessionResponse joinNote(UUID noteId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        // Check user has access to this note's organization
        authorizationService.checkOrganizationAccess(note.getOrganizationId(), user.getId());

        // Get or create session
        NoteSession session = activeSessions.computeIfAbsent(noteId, id -> createNewSession(note));

        // Add collaborator if not already present
        if (!session.hasCollaborator(user.getId())) {
            NoteCollaborator collaborator = NoteCollaborator.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .profilePhotoUrl(user.getProfilePhotoUrl())
                    .color(assignColor(session.getCollaboratorCount()))
                    .joinedAt(LocalDateTime.now())
                    .lastActiveAt(LocalDateTime.now())
                    .build();
            session.addCollaborator(collaborator);
            log.info("User {} joined note {} collaboration session", userEmail, noteId);
        } else {
            // Update last active time
            NoteCollaborator existing = session.getCollaborators().get(user.getId());
            existing.setLastActiveAt(LocalDateTime.now());
        }

        return toSessionResponse(session);
    }

    /**
     * User leaves a note editing session.
     */
    public NoteCollaborationEventResponse leaveNote(UUID noteId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        NoteSession session = activeSessions.get(noteId);
        if (session == null) {
            log.warn("No active session found for note {}", noteId);
            return null;
        }

        NoteCollaborator collaborator = session.getCollaborators().get(user.getId());
        if (collaborator == null) {
            log.warn("User {} not found in session for note {}", userEmail, noteId);
            return null;
        }

        session.removeCollaborator(user.getId());
        log.info("User {} left note {} collaboration session", userEmail, noteId);

        // Clean up empty sessions
        if (session.getCollaboratorCount() == 0) {
            activeSessions.remove(noteId);
            log.info("Removed empty session for note {}", noteId);
        }

        return NoteCollaborationEventResponse.builder()
                .eventType(NoteCollaborationEventResponse.EventType.USER_LEFT)
                .noteId(noteId)
                .collaborator(toCollaboratorResponse(collaborator))
                .totalCollaborators(session.getCollaboratorCount())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Process an edit from a user.
     * Simple conflict detection based on version numbers.
     */
    public NoteEditResponse processEdit(NoteEditRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        NoteSession session = activeSessions.get(request.getNoteId());
        if (session == null) {
            log.warn("No active session found for note {}", request.getNoteId());
            return null;
        }

        // Update collaborator activity
        NoteCollaborator collaborator = session.getCollaborators().get(user.getId());
        if (collaborator != null) {
            collaborator.setLastActiveAt(LocalDateTime.now());
        }

        // Simple conflict detection
        boolean hasConflict = false;
        String conflictMessage = null;

        if (request.getBaseVersion() != null && session.getVersion() != null
                && request.getBaseVersion() < session.getVersion()) {
            hasConflict = true;
            conflictMessage = "Your changes are based on an older version. Please refresh and try again.";
            log.warn("Conflict detected for user {} on note {}: base version {} < current version {}",
                    userEmail, request.getNoteId(), request.getBaseVersion(), session.getVersion());
        }

        // Update session content if no conflict
        if (!hasConflict) {
            if (request.getType() == NoteEditOperation.EditType.TITLE_UPDATE && request.getTitle() != null) {
                session.setCurrentTitle(request.getTitle());
            }
            if (request.getType() == NoteEditOperation.EditType.CONTENT_UPDATE && request.getContent() != null) {
                session.setCurrentContent(request.getContent());
            }
            if (request.getType() == NoteEditOperation.EditType.FULL_UPDATE) {
                if (request.getTitle() != null) session.setCurrentTitle(request.getTitle());
                if (request.getContent() != null) session.setCurrentContent(request.getContent());
            }
            session.incrementVersion();
        }

        return NoteEditResponse.builder()
                .noteId(request.getNoteId())
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(collaborator != null ? collaborator.getDisplayName() : user.getEmail())
                .type(request.getType())
                .title(request.getTitle())
                .content(request.getContent())
                .version(session.getVersion())
                .timestamp(LocalDateTime.now())
                .hasConflict(hasConflict)
                .conflictMessage(conflictMessage)
                .build();
    }

    /**
     * Update cursor position for a user.
     */
    public NoteCursorResponse updateCursor(NoteCursorRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        NoteSession session = activeSessions.get(request.getNoteId());
        if (session == null) {
            return null;
        }

        NoteCollaborator collaborator = session.getCollaborators().get(user.getId());
        if (collaborator == null) {
            return null;
        }

        collaborator.setLastActiveAt(LocalDateTime.now());

        CursorPosition cursorPosition = CursorPosition.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(collaborator.getDisplayName())
                .color(collaborator.getColor())
                .position(request.getPosition())
                .selectionStart(request.getSelectionStart())
                .selectionEnd(request.getSelectionEnd())
                .build();

        session.updateCursorPosition(cursorPosition);

        return toCursorResponse(cursorPosition);
    }

    /**
     * Get typing indicator event.
     */
    public NoteCollaborationEventResponse createTypingEvent(UUID noteId, String userEmail, boolean isTyping) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        NoteSession session = activeSessions.get(noteId);
        if (session == null) {
            return null;
        }

        NoteCollaborator collaborator = session.getCollaborators().get(user.getId());
        if (collaborator == null) {
            return null;
        }

        return NoteCollaborationEventResponse.builder()
                .eventType(isTyping
                        ? NoteCollaborationEventResponse.EventType.USER_TYPING
                        : NoteCollaborationEventResponse.EventType.USER_STOPPED_TYPING)
                .noteId(noteId)
                .collaborator(toCollaboratorResponse(collaborator))
                .totalCollaborators(session.getCollaboratorCount())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Get current session for a note.
     */
    public NoteSessionResponse getSession(UUID noteId) {
        NoteSession session = activeSessions.get(noteId);
        if (session == null) {
            return null;
        }
        return toSessionResponse(session);
    }

    /**
     * Get collaborators for a note.
     */
    public List<NoteCollaboratorResponse> getCollaborators(UUID noteId) {
        NoteSession session = activeSessions.get(noteId);
        if (session == null) {
            return Collections.emptyList();
        }
        return session.getCollaborators().values().stream()
                .map(this::toCollaboratorResponse)
                .toList();
    }

    /**
     * Get active collaborator count for a note.
     */
    public int getCollaboratorCount(UUID noteId) {
        NoteSession session = activeSessions.get(noteId);
        return session != null ? session.getCollaboratorCount() : 0;
    }

    /**
     * Handle user disconnect (cleanup).
     */
    public void handleUserDisconnect(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return;
        }

        // Remove user from all sessions
        activeSessions.forEach((noteId, session) -> {
            if (session.hasCollaborator(user.getId())) {
                session.removeCollaborator(user.getId());
                log.info("User {} disconnected from note {} session", userEmail, noteId);

                // Clean up empty sessions
                if (session.getCollaboratorCount() == 0) {
                    activeSessions.remove(noteId);
                    log.info("Removed empty session for note {}", noteId);
                }
            }
        });
    }

    // Helper methods

    private NoteSession createNewSession(Note note) {
        return NoteSession.builder()
                .noteId(note.getId())
                .organizationId(note.getOrganizationId())
                .currentTitle(note.getTitle())
                .currentContent(note.getContent())
                .version(1L)
                .createdAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .collaborators(new ConcurrentHashMap<>())
                .cursorPositions(new ConcurrentHashMap<>())
                .build();
    }

    private String assignColor(int index) {
        return COLLABORATOR_COLORS.get(index % COLLABORATOR_COLORS.size());
    }

    private NoteSessionResponse toSessionResponse(NoteSession session) {
        List<NoteCollaboratorResponse> collaborators = session.getCollaborators().values().stream()
                .map(this::toCollaboratorResponse)
                .toList();

        List<NoteCursorResponse> cursors = session.getCursorPositions().values().stream()
                .map(this::toCursorResponse)
                .toList();

        return NoteSessionResponse.builder()
                .noteId(session.getNoteId())
                .organizationId(session.getOrganizationId())
                .currentTitle(session.getCurrentTitle())
                .currentContent(session.getCurrentContent())
                .version(session.getVersion())
                .lastModifiedAt(session.getLastModifiedAt())
                .collaborators(collaborators)
                .cursors(cursors)
                .build();
    }

    private NoteCollaboratorResponse toCollaboratorResponse(NoteCollaborator collaborator) {
        return NoteCollaboratorResponse.builder()
                .userId(collaborator.getUserId())
                .email(collaborator.getEmail())
                .firstName(collaborator.getFirstName())
                .lastName(collaborator.getLastName())
                .displayName(collaborator.getDisplayName())
                .initials(collaborator.getInitials())
                .profilePhotoUrl(collaborator.getProfilePhotoUrl())
                .color(collaborator.getColor())
                .joinedAt(collaborator.getJoinedAt())
                .lastActiveAt(collaborator.getLastActiveAt())
                .build();
    }

    private NoteCursorResponse toCursorResponse(CursorPosition cursor) {
        return NoteCursorResponse.builder()
                .userId(cursor.getUserId())
                .email(cursor.getEmail())
                .displayName(cursor.getDisplayName())
                .color(cursor.getColor())
                .position(cursor.getPosition())
                .selectionStart(cursor.getSelectionStart())
                .selectionEnd(cursor.getSelectionEnd())
                .build();
    }
}
