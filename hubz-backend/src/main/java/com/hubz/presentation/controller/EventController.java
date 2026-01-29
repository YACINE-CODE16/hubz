package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateEventRequest;
import com.hubz.application.dto.request.UpdateEventRequest;
import com.hubz.application.dto.response.EventResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.EventService;
import com.hubz.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping("/api/organizations/{orgId}/events")
    public ResponseEntity<List<EventResponse>> getByOrganization(
            @PathVariable UUID orgId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);

        if (start != null && end != null) {
            return ResponseEntity.ok(
                    eventService.getByOrganizationAndTimeRange(orgId, start, end, currentUserId)
            );
        }

        return ResponseEntity.ok(eventService.getByOrganization(orgId, currentUserId));
    }

    @PostMapping("/api/organizations/{orgId}/events")
    public ResponseEntity<EventResponse> createOrganizationEvent(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateEventRequest request,
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.create(request, orgId, userId));
    }

    @GetMapping("/api/users/me/events")
    public ResponseEntity<List<EventResponse>> getPersonalEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);

        if (start != null && end != null) {
            return ResponseEntity.ok(
                    eventService.getPersonalEventsByTimeRange(userId, start, end)
            );
        }

        return ResponseEntity.ok(eventService.getPersonalEvents(userId));
    }

    @PostMapping("/api/users/me/events")
    public ResponseEntity<EventResponse> createPersonalEvent(
            @Valid @RequestBody CreateEventRequest request,
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.create(request, null, userId));
    }

    @PutMapping("/api/events/{id}")
    public ResponseEntity<EventResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(eventService.update(id, request, currentUserId));
    }

    @DeleteMapping("/api/events/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        eventService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
