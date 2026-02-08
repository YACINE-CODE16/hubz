package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateNoteFolderRequest;
import com.hubz.application.dto.request.UpdateNoteFolderRequest;
import com.hubz.application.dto.response.NoteFolderResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.NoteFolderService;
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
public class NoteFolderController {

    private final NoteFolderService noteFolderService;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping("/api/organizations/{orgId}/note-folders")
    public ResponseEntity<List<NoteFolderResponse>> getByOrganization(
            @PathVariable UUID orgId,
            @RequestParam(required = false, defaultValue = "false") boolean flat,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);

        if (flat) {
            return ResponseEntity.ok(noteFolderService.getFlatList(orgId, currentUserId));
        }

        return ResponseEntity.ok(noteFolderService.getByOrganization(orgId, currentUserId));
    }

    @GetMapping("/api/note-folders/{id}")
    public ResponseEntity<NoteFolderResponse> getById(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(noteFolderService.getById(id, currentUserId));
    }

    @PostMapping("/api/organizations/{orgId}/note-folders")
    public ResponseEntity<NoteFolderResponse> create(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateNoteFolderRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteFolderService.create(request, orgId, currentUserId));
    }

    @PutMapping("/api/note-folders/{id}")
    public ResponseEntity<NoteFolderResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNoteFolderRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(noteFolderService.update(id, request, currentUserId));
    }

    @DeleteMapping("/api/note-folders/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        noteFolderService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
