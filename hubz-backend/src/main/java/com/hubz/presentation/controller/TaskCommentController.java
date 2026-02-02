package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateTaskCommentRequest;
import com.hubz.application.dto.request.UpdateTaskCommentRequest;
import com.hubz.application.dto.response.TaskCommentResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.TaskCommentService;
import com.hubz.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class TaskCommentController {

    private final TaskCommentService commentService;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping
    public ResponseEntity<List<TaskCommentResponse>> getComments(
            @PathVariable UUID taskId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(commentService.getCommentsByTask(taskId, currentUserId));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getCommentCount(
            @PathVariable UUID taskId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        int count = commentService.getCommentCount(taskId, currentUserId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping
    public ResponseEntity<TaskCommentResponse> createComment(
            @PathVariable UUID taskId,
            @Valid @RequestBody CreateTaskCommentRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(taskId, request, currentUserId));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<TaskCommentResponse> updateComment(
            @PathVariable UUID taskId,
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateTaskCommentRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(commentService.updateComment(commentId, request, currentUserId));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID taskId,
            @PathVariable UUID commentId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        commentService.deleteComment(commentId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
