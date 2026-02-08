package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateTaskRequest;
import com.hubz.application.dto.request.UpdateTaskRequest;
import com.hubz.application.dto.request.UpdateTaskStatusRequest;
import com.hubz.application.dto.response.MessageResponse;
import com.hubz.application.dto.response.TaskResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.TaskService;
import com.hubz.domain.exception.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Tasks", description = "Task management with Kanban support")
public class TaskController {

    private final TaskService taskService;
    private final UserRepositoryPort userRepositoryPort;

    @Operation(
            summary = "Get tasks by organization",
            description = "Returns all tasks for a specific organization. User must be a member of the organization."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - not a member"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @GetMapping("/api/organizations/{orgId}/tasks")
    public ResponseEntity<List<TaskResponse>> getByOrganization(
            @Parameter(description = "Organization ID") @PathVariable UUID orgId,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(taskService.getByOrganization(orgId, currentUserId));
    }

    @Operation(
            summary = "Create task",
            description = "Creates a new task in the specified organization. User must be a member with appropriate permissions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task created successfully",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @PostMapping("/api/organizations/{orgId}/tasks")
    public ResponseEntity<TaskResponse> create(
            @Parameter(description = "Organization ID") @PathVariable UUID orgId,
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication) {
        UUID creatorId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.create(request, orgId, creatorId));
    }

    @Operation(
            summary = "Update task",
            description = "Updates a task's details. User must have appropriate permissions in the organization."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task updated successfully",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PutMapping("/api/tasks/{id}")
    public ResponseEntity<TaskResponse> update(
            @Parameter(description = "Task ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(taskService.update(id, request, currentUserId));
    }

    @Operation(
            summary = "Update task status",
            description = "Changes only the status of a task (TODO, IN_PROGRESS, DONE). Useful for Kanban drag-and-drop."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PatchMapping("/api/tasks/{id}/status")
    public ResponseEntity<TaskResponse> updateStatus(
            @Parameter(description = "Task ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskStatusRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(taskService.updateStatus(id, request, currentUserId));
    }

    @Operation(
            summary = "Delete task",
            description = "Permanently deletes a task. User must have appropriate permissions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @DeleteMapping("/api/tasks/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Task ID") @PathVariable UUID id,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        taskService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get my tasks",
            description = "Returns all tasks assigned to the current user across all organizations."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
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
