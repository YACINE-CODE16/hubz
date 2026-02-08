package com.hubz.presentation.controller;

import com.hubz.application.dto.request.NoteCursorRequest;
import com.hubz.application.dto.request.NoteEditRequest;
import com.hubz.application.dto.request.NoteJoinRequest;
import com.hubz.application.dto.request.NoteLeaveRequest;
import com.hubz.application.dto.response.*;
import com.hubz.application.service.NoteCollaborationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * WebSocket controller for real-time note collaboration.
 * Handles STOMP messages for joining, editing, cursor updates, and leaving notes.
 */
@Controller
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@Slf4j
public class NoteCollaborationController {

    private final NoteCollaborationService collaborationService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * User joins a note editing session.
     * Client subscribes to /topic/note/{noteId} to receive updates.
     */
    @MessageMapping("/note/join")
    @SendToUser("/queue/note/session")
    public NoteSessionResponse joinNote(@Payload NoteJoinRequest request, Principal principal) {
        String userEmail = principal.getName();
        log.info("User {} joining note {}", userEmail, request.getNoteId());

        NoteSessionResponse session = collaborationService.joinNote(request.getNoteId(), userEmail);

        // Broadcast join event to all collaborators
        NoteCollaboratorResponse collaborator = session.getCollaborators().stream()
                .filter(c -> c.getEmail().equals(userEmail))
                .findFirst()
                .orElse(null);

        if (collaborator != null) {
            NoteCollaborationEventResponse event = NoteCollaborationEventResponse.builder()
                    .eventType(NoteCollaborationEventResponse.EventType.USER_JOINED)
                    .noteId(request.getNoteId())
                    .collaborator(collaborator)
                    .totalCollaborators(session.getCollaborators().size())
                    .timestamp(session.getLastModifiedAt())
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/note/" + request.getNoteId() + "/events",
                    event
            );
        }

        return session;
    }

    /**
     * User leaves a note editing session.
     */
    @MessageMapping("/note/leave")
    public void leaveNote(@Payload NoteLeaveRequest request, Principal principal) {
        String userEmail = principal.getName();
        log.info("User {} leaving note {}", userEmail, request.getNoteId());

        NoteCollaborationEventResponse event = collaborationService.leaveNote(request.getNoteId(), userEmail);

        if (event != null) {
            messagingTemplate.convertAndSend(
                    "/topic/note/" + request.getNoteId() + "/events",
                    event
            );
        }
    }

    /**
     * Process an edit from a user and broadcast to all collaborators.
     */
    @MessageMapping("/note/edit")
    public void editNote(@Payload NoteEditRequest request, Principal principal) {
        String userEmail = principal.getName();
        log.debug("User {} editing note {}", userEmail, request.getNoteId());

        NoteEditResponse response = collaborationService.processEdit(request, userEmail);

        if (response != null) {
            // Broadcast edit to all collaborators
            messagingTemplate.convertAndSend(
                    "/topic/note/" + request.getNoteId() + "/edits",
                    response
            );

            // If conflict, send error to the user
            if (response.isHasConflict()) {
                messagingTemplate.convertAndSendToUser(
                        userEmail,
                        "/queue/note/errors",
                        response
                );
            }
        }
    }

    /**
     * Update cursor position and broadcast to all collaborators.
     */
    @MessageMapping("/note/cursor")
    public void updateCursor(@Payload NoteCursorRequest request, Principal principal) {
        String userEmail = principal.getName();

        NoteCursorResponse cursor = collaborationService.updateCursor(request, userEmail);

        if (cursor != null) {
            messagingTemplate.convertAndSend(
                    "/topic/note/" + request.getNoteId() + "/cursors",
                    cursor
            );
        }
    }

    /**
     * User is typing indicator.
     */
    @MessageMapping("/note/typing")
    public void userTyping(@Payload NoteJoinRequest request, Principal principal) {
        String userEmail = principal.getName();

        NoteCollaborationEventResponse event = collaborationService.createTypingEvent(
                request.getNoteId(), userEmail, true
        );

        if (event != null) {
            messagingTemplate.convertAndSend(
                    "/topic/note/" + request.getNoteId() + "/events",
                    event
            );
        }
    }

    /**
     * User stopped typing indicator.
     */
    @MessageMapping("/note/stopped-typing")
    public void userStoppedTyping(@Payload NoteJoinRequest request, Principal principal) {
        String userEmail = principal.getName();

        NoteCollaborationEventResponse event = collaborationService.createTypingEvent(
                request.getNoteId(), userEmail, false
        );

        if (event != null) {
            messagingTemplate.convertAndSend(
                    "/topic/note/" + request.getNoteId() + "/events",
                    event
            );
        }
    }

    /**
     * Handle WebSocket disconnect - clean up user from all sessions.
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            String userEmail = principal.getName();
            log.info("User {} disconnected from WebSocket", userEmail);
            collaborationService.handleUserDisconnect(userEmail);
        }
    }

    // REST endpoints for getting collaboration info

    /**
     * Get current collaborators for a note (REST endpoint).
     */
    @GetMapping("/{noteId}/collaborators")
    @ResponseBody
    public ResponseEntity<List<NoteCollaboratorResponse>> getCollaborators(
            @PathVariable UUID noteId,
            Authentication authentication
    ) {
        List<NoteCollaboratorResponse> collaborators = collaborationService.getCollaborators(noteId);
        return ResponseEntity.ok(collaborators);
    }

    /**
     * Get collaborator count for a note (REST endpoint).
     */
    @GetMapping("/{noteId}/collaborators/count")
    @ResponseBody
    public ResponseEntity<Integer> getCollaboratorCount(@PathVariable UUID noteId) {
        int count = collaborationService.getCollaboratorCount(noteId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get current session for a note (REST endpoint).
     */
    @GetMapping("/{noteId}/session")
    @ResponseBody
    public ResponseEntity<NoteSessionResponse> getSession(
            @PathVariable UUID noteId,
            Authentication authentication
    ) {
        NoteSessionResponse session = collaborationService.getSession(noteId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }
}
