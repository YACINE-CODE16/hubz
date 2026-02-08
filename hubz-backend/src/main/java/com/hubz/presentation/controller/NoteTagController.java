package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateNoteTagRequest;
import com.hubz.application.dto.request.UpdateNoteTagRequest;
import com.hubz.application.dto.response.NoteTagResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.NoteTagService;
import com.hubz.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class NoteTagController {

    private final NoteTagService noteTagService;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping("/api/organizations/{orgId}/note-tags")
    public ResponseEntity<List<NoteTagResponse>> getByOrganization(
            @PathVariable UUID orgId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(noteTagService.getByOrganization(orgId, currentUserId));
    }

    @GetMapping("/api/note-tags/{id}")
    public ResponseEntity<NoteTagResponse> getById(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(noteTagService.getById(id, currentUserId));
    }

    @PostMapping("/api/organizations/{orgId}/note-tags")
    public ResponseEntity<NoteTagResponse> create(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateNoteTagRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteTagService.create(request, orgId, currentUserId));
    }

    @PutMapping("/api/note-tags/{id}")
    public ResponseEntity<NoteTagResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNoteTagRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(noteTagService.update(id, request, currentUserId));
    }

    @DeleteMapping("/api/note-tags/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        noteTagService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    // Note-Tag relationship endpoints

    @GetMapping("/api/notes/{noteId}/tags")
    public ResponseEntity<List<NoteTagResponse>> getTagsByNote(
            @PathVariable UUID noteId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(noteTagService.getTagsByNote(noteId, currentUserId));
    }

    @PostMapping("/api/notes/{noteId}/tags/{tagId}")
    public ResponseEntity<Void> addTagToNote(
            @PathVariable UUID noteId,
            @PathVariable UUID tagId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        noteTagService.addTagToNote(noteId, tagId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/notes/{noteId}/tags/{tagId}")
    public ResponseEntity<Void> removeTagFromNote(
            @PathVariable UUID noteId,
            @PathVariable UUID tagId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        noteTagService.removeTagFromNote(noteId, tagId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/notes/{noteId}/tags")
    public ResponseEntity<Void> setNoteTags(
            @PathVariable UUID noteId,
            @RequestBody Set<UUID> tagIds,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        noteTagService.setNoteTags(noteId, tagIds, currentUserId);
        return ResponseEntity.ok().build();
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
