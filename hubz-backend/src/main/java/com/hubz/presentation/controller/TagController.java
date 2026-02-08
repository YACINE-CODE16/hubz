package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateTagRequest;
import com.hubz.application.dto.request.UpdateTagRequest;
import com.hubz.application.dto.response.TagResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.TagService;
import com.hubz.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping("/api/organizations/{orgId}/tags")
    public ResponseEntity<List<TagResponse>> getByOrganization(
            @PathVariable UUID orgId, Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(tagService.getByOrganization(orgId, currentUserId));
    }

    @PostMapping("/api/organizations/{orgId}/tags")
    public ResponseEntity<TagResponse> create(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateTagRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tagService.create(request, orgId, currentUserId));
    }

    @GetMapping("/api/tags/{id}")
    public ResponseEntity<TagResponse> getById(
            @PathVariable UUID id, Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(tagService.getById(id, currentUserId));
    }

    @PutMapping("/api/tags/{id}")
    public ResponseEntity<TagResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTagRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(tagService.update(id, request, currentUserId));
    }

    @DeleteMapping("/api/tags/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id, Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        tagService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    // Task-Tag endpoints

    @GetMapping("/api/tasks/{taskId}/tags")
    public ResponseEntity<List<TagResponse>> getTagsByTask(
            @PathVariable UUID taskId, Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(tagService.getTagsByTask(taskId, currentUserId));
    }

    @PostMapping("/api/tasks/{taskId}/tags/{tagId}")
    public ResponseEntity<Void> addTagToTask(
            @PathVariable UUID taskId,
            @PathVariable UUID tagId,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        tagService.addTagToTask(taskId, tagId, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/tasks/{taskId}/tags/{tagId}")
    public ResponseEntity<Void> removeTagFromTask(
            @PathVariable UUID taskId,
            @PathVariable UUID tagId,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        tagService.removeTagFromTask(taskId, tagId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/tasks/{taskId}/tags")
    public ResponseEntity<Void> setTaskTags(
            @PathVariable UUID taskId,
            @RequestBody Set<UUID> tagIds,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        tagService.setTaskTags(taskId, tagIds, currentUserId);
        return ResponseEntity.ok().build();
    }

    // Document-Tag endpoints

    @GetMapping("/api/documents/{documentId}/tags")
    public ResponseEntity<List<TagResponse>> getTagsByDocument(
            @PathVariable UUID documentId, Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(tagService.getTagsByDocument(documentId, currentUserId));
    }

    @PostMapping("/api/documents/{documentId}/tags/{tagId}")
    public ResponseEntity<Void> addTagToDocument(
            @PathVariable UUID documentId,
            @PathVariable UUID tagId,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        tagService.addTagToDocument(documentId, tagId, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/documents/{documentId}/tags/{tagId}")
    public ResponseEntity<Void> removeTagFromDocument(
            @PathVariable UUID documentId,
            @PathVariable UUID tagId,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        tagService.removeTagFromDocument(documentId, tagId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/documents/{documentId}/tags")
    public ResponseEntity<Void> setDocumentTags(
            @PathVariable UUID documentId,
            @RequestBody Set<UUID> tagIds,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        tagService.setDocumentTags(documentId, tagIds, currentUserId);
        return ResponseEntity.ok().build();
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
