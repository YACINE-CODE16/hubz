package com.hubz.presentation.controller;

import com.hubz.application.dto.request.SendDirectMessageRequest;
import com.hubz.application.dto.request.UpdateDirectMessageRequest;
import com.hubz.application.dto.response.ConversationResponse;
import com.hubz.application.dto.response.DirectMessageResponse;
import com.hubz.application.dto.response.UnreadCountResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.DirectMessageService;
import com.hubz.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class DirectMessageController {

    private final DirectMessageService directMessageService;
    private final UserRepositoryPort userRepositoryPort;

    @PostMapping
    public ResponseEntity<DirectMessageResponse> sendMessage(
            @Valid @RequestBody SendDirectMessageRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(directMessageService.sendMessage(request, currentUserId));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getConversations(
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(directMessageService.getConversations(currentUserId));
    }

    @GetMapping("/conversation/{userId}")
    public ResponseEntity<Page<DirectMessageResponse>> getConversation(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(directMessageService.getConversation(currentUserId, userId, page, size));
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<DirectMessageResponse> editMessage(
            @PathVariable UUID messageId,
            @Valid @RequestBody UpdateDirectMessageRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(directMessageService.editMessage(messageId, request, currentUserId));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID messageId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        directMessageService.deleteMessage(messageId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{messageId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID messageId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        directMessageService.markAsRead(messageId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/conversation/{userId}/read")
    public ResponseEntity<Void> markConversationAsRead(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        directMessageService.markConversationAsRead(currentUserId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread/count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(directMessageService.getUnreadCount(currentUserId));
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
