package com.hubz.presentation.controller;

import com.hubz.application.dto.request.UpdateNotificationPreferencesRequest;
import com.hubz.application.dto.response.NotificationCountResponse;
import com.hubz.application.dto.response.NotificationPreferencesResponse;
import com.hubz.application.dto.response.NotificationResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.NotificationPreferencesService;
import com.hubz.application.service.NotificationService;
import com.hubz.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationPreferencesService preferencesService;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.ok(notificationService.getNotifications(userId, limit));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @GetMapping("/count")
    public ResponseEntity<NotificationCountResponse> getUnreadCount(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        notificationService.deleteAllNotifications(userId);
        return ResponseEntity.noContent().build();
    }

    // Preferences endpoints

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferencesResponse> getPreferences(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(preferencesService.getPreferences(email));
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
            @Valid @RequestBody UpdateNotificationPreferencesRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(preferencesService.updatePreferences(email, request));
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
