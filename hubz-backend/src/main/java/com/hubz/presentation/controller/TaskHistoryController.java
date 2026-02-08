package com.hubz.presentation.controller;

import com.hubz.application.dto.response.TaskHistoryResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.TaskHistoryService;
import com.hubz.domain.enums.TaskHistoryField;
import com.hubz.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TaskHistoryController {

    private final TaskHistoryService taskHistoryService;
    private final UserRepositoryPort userRepositoryPort;

    /**
     * Get the history of changes for a task.
     * Optionally filter by field type.
     */
    @GetMapping("/api/tasks/{taskId}/history")
    public ResponseEntity<List<TaskHistoryResponse>> getTaskHistory(
            @PathVariable UUID taskId,
            @RequestParam(required = false) TaskHistoryField field,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);

        List<TaskHistoryResponse> history;
        if (field != null) {
            history = taskHistoryService.getTaskHistoryByField(taskId, field, currentUserId);
        } else {
            history = taskHistoryService.getTaskHistory(taskId, currentUserId);
        }

        return ResponseEntity.ok(history);
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
