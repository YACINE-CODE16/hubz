package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateTaskRequest;
import com.hubz.application.dto.request.UpdateTaskRequest;
import com.hubz.application.dto.request.UpdateTaskStatusRequest;
import com.hubz.application.dto.response.TaskResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.TaskService;
import com.hubz.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping("/api/organizations/{orgId}/tasks")
    public ResponseEntity<List<TaskResponse>> getByOrganization(
            @PathVariable UUID orgId, Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(taskService.getByOrganization(orgId, currentUserId));
    }

    @PostMapping("/api/organizations/{orgId}/tasks")
    public ResponseEntity<TaskResponse> create(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication) {
        UUID creatorId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.create(request, orgId, creatorId));
    }

    @PutMapping("/api/tasks/{id}")
    public ResponseEntity<TaskResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(taskService.update(id, request, currentUserId));
    }

    @PatchMapping("/api/tasks/{id}/status")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskStatusRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(taskService.updateStatus(id, request, currentUserId));
    }

    @DeleteMapping("/api/tasks/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        taskService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/users/me/tasks")
    public ResponseEntity<List<TaskResponse>> getMyTasks(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.ok(taskService.getByUser(userId));
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
