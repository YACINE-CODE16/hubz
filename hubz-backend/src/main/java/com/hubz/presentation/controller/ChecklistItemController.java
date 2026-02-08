package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateChecklistItemRequest;
import com.hubz.application.dto.request.ReorderChecklistItemsRequest;
import com.hubz.application.dto.request.UpdateChecklistItemRequest;
import com.hubz.application.dto.response.ChecklistItemResponse;
import com.hubz.application.dto.response.ChecklistProgressResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.ChecklistItemService;
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
@RequestMapping("/api/tasks/{taskId}/checklist")
@RequiredArgsConstructor
public class ChecklistItemController {

    private final ChecklistItemService checklistService;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping
    public ResponseEntity<ChecklistProgressResponse> getChecklist(
            @PathVariable UUID taskId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(checklistService.getChecklist(taskId, currentUserId));
    }

    @PostMapping
    public ResponseEntity<ChecklistItemResponse> createItem(
            @PathVariable UUID taskId,
            @Valid @RequestBody CreateChecklistItemRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checklistService.create(taskId, request, currentUserId));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ChecklistItemResponse> updateItem(
            @PathVariable UUID taskId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateChecklistItemRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(checklistService.update(itemId, request, currentUserId));
    }

    @PatchMapping("/{itemId}/toggle")
    public ResponseEntity<ChecklistItemResponse> toggleItem(
            @PathVariable UUID taskId,
            @PathVariable UUID itemId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(checklistService.toggleCompleted(itemId, currentUserId));
    }

    @PutMapping("/reorder")
    public ResponseEntity<List<ChecklistItemResponse>> reorderItems(
            @PathVariable UUID taskId,
            @Valid @RequestBody ReorderChecklistItemsRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(checklistService.reorder(taskId, request, currentUserId));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable UUID taskId,
            @PathVariable UUID itemId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        checklistService.delete(itemId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
