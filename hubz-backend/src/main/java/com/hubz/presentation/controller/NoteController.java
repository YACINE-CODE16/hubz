package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateNoteRequest;
import com.hubz.application.dto.request.UpdateNoteRequest;
import com.hubz.application.dto.response.NoteResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.NoteService;
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
public class NoteController {

    private final NoteService noteService;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping("/api/organizations/{orgId}/notes")
    public ResponseEntity<List<NoteResponse>> getByOrganization(
            @PathVariable UUID orgId,
            @RequestParam(required = false) String category,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);

        if (category != null && !category.isEmpty()) {
            return ResponseEntity.ok(
                    noteService.getByOrganizationAndCategory(orgId, category, currentUserId)
            );
        }

        return ResponseEntity.ok(noteService.getByOrganization(orgId, currentUserId));
    }

    @PostMapping("/api/organizations/{orgId}/notes")
    public ResponseEntity<NoteResponse> create(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateNoteRequest request,
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.create(request, orgId, userId));
    }

    @PutMapping("/api/notes/{id}")
    public ResponseEntity<NoteResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNoteRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(noteService.update(id, request, currentUserId));
    }

    @DeleteMapping("/api/notes/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        noteService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
