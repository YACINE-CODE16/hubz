package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateMessageRequest;
import com.hubz.application.dto.request.UpdateMessageRequest;
import com.hubz.application.dto.response.ChatMessageResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.TeamChatService;
import com.hubz.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TeamChatController {

    private final TeamChatService teamChatService;
    private final UserRepositoryPort userRepositoryPort;

    @PostMapping("/api/teams/{teamId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable UUID teamId,
            @Valid @RequestBody CreateMessageRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamChatService.sendMessage(teamId, request, currentUserId));
    }

    @GetMapping("/api/teams/{teamId}/messages")
    public ResponseEntity<Page<ChatMessageResponse>> getMessages(
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(teamChatService.getMessages(teamId, page, size, currentUserId));
    }

    @PutMapping("/api/teams/{teamId}/messages/{messageId}")
    public ResponseEntity<ChatMessageResponse> editMessage(
            @PathVariable UUID teamId,
            @PathVariable UUID messageId,
            @Valid @RequestBody UpdateMessageRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(teamChatService.editMessage(messageId, request, currentUserId));
    }

    @DeleteMapping("/api/teams/{teamId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID teamId,
            @PathVariable UUID messageId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        teamChatService.deleteMessage(messageId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/teams/{teamId}/messages/count")
    public ResponseEntity<Integer> getMessageCount(
            @PathVariable UUID teamId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(teamChatService.getMessageCount(teamId, currentUserId));
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
