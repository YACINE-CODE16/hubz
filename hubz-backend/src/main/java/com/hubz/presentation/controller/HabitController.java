package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateHabitRequest;
import com.hubz.application.dto.request.LogHabitRequest;
import com.hubz.application.dto.request.UpdateHabitRequest;
import com.hubz.application.dto.response.HabitLogResponse;
import com.hubz.application.dto.response.HabitResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.HabitService;
import com.hubz.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping("/api/users/me/habits")
    public ResponseEntity<List<HabitResponse>> getUserHabits(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.ok(habitService.getUserHabits(userId));
    }

    @PostMapping("/api/users/me/habits")
    public ResponseEntity<HabitResponse> createHabit(
            @Valid @RequestBody CreateHabitRequest request,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(habitService.create(request, userId));
    }

    @PutMapping("/api/habits/{id}")
    public ResponseEntity<HabitResponse> updateHabit(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateHabitRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(habitService.update(id, request, currentUserId));
    }

    @DeleteMapping("/api/habits/{id}")
    public ResponseEntity<Void> deleteHabit(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        habitService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/habits/{id}/log")
    public ResponseEntity<HabitLogResponse> logHabit(
            @PathVariable UUID id,
            @Valid @RequestBody LogHabitRequest request,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(habitService.logHabit(id, request, userId));
    }

    @GetMapping("/api/habits/{id}/logs")
    public ResponseEntity<List<HabitLogResponse>> getHabitLogs(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.ok(habitService.getHabitLogs(id, userId));
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
