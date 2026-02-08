package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateGoalRequest;
import com.hubz.application.dto.request.UpdateGoalRequest;
import com.hubz.application.dto.response.GoalAnalyticsResponse;
import com.hubz.application.dto.response.GoalResponse;
import com.hubz.application.dto.response.MessageResponse;
import com.hubz.application.dto.response.TaskResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.GoalAnalyticsService;
import com.hubz.application.service.GoalService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Goals", description = "Goal tracking and progress management")
public class GoalController {

    private final GoalService goalService;
    private final GoalAnalyticsService goalAnalyticsService;
    private final UserRepositoryPort userRepositoryPort;

    @Operation(
            summary = "Get organization goals",
            description = "Returns all goals for a specific organization. User must be a member of the organization."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Goals retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GoalResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - not a member"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @GetMapping("/api/organizations/{orgId}/goals")
    public ResponseEntity<List<GoalResponse>> getByOrganization(
            @Parameter(description = "Organization ID") @PathVariable UUID orgId,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(goalService.getByOrganization(orgId, currentUserId));
    }

    @Operation(
            summary = "Create organization goal",
            description = "Creates a new goal in the specified organization."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Goal created successfully",
                    content = @Content(schema = @Schema(implementation = GoalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @PostMapping("/api/organizations/{orgId}/goals")
    public ResponseEntity<GoalResponse> createOrganizationGoal(
            @Parameter(description = "Organization ID") @PathVariable UUID orgId,
            @Valid @RequestBody CreateGoalRequest request,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalService.create(request, orgId, userId));
    }

    @Operation(
            summary = "Get personal goals",
            description = "Returns all personal goals for the current user (not associated with any organization)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Goals retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GoalResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/api/users/me/goals")
    public ResponseEntity<List<GoalResponse>> getPersonalGoals(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.ok(goalService.getPersonalGoals(userId));
    }

    @Operation(
            summary = "Create personal goal",
            description = "Creates a new personal goal for the current user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Goal created successfully",
                    content = @Content(schema = @Schema(implementation = GoalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/api/users/me/goals")
    public ResponseEntity<GoalResponse> createPersonalGoal(
            @Valid @RequestBody CreateGoalRequest request,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalService.create(request, null, userId));
    }

    @Operation(
            summary = "Update goal",
            description = "Updates a goal's details including progress. User must own the goal or be an organization member."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Goal updated successfully",
                    content = @Content(schema = @Schema(implementation = GoalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Goal not found")
    })
    @PutMapping("/api/goals/{id}")
    public ResponseEntity<GoalResponse> update(
            @Parameter(description = "Goal ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateGoalRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(goalService.update(id, request, currentUserId));
    }

    @Operation(
            summary = "Delete goal",
            description = "Permanently deletes a goal. User must own the goal or have appropriate permissions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Goal deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Goal not found")
    })
    @DeleteMapping("/api/goals/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Goal ID") @PathVariable UUID id,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        goalService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get goal by ID",
            description = "Returns a specific goal with its details and linked task statistics."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Goal retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GoalResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Goal not found")
    })
    @GetMapping("/api/goals/{id}")
    public ResponseEntity<GoalResponse> getById(
            @Parameter(description = "Goal ID") @PathVariable UUID id,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(goalService.getById(id, currentUserId));
    }

    @Operation(
            summary = "Get tasks linked to goal",
            description = "Returns all tasks that are linked to a specific goal."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Goal not found")
    })
    @GetMapping("/api/goals/{id}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasksByGoal(
            @Parameter(description = "Goal ID") @PathVariable UUID id,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(goalService.getTasksByGoal(id, currentUserId));
    }

    @Operation(
            summary = "Get personal goal analytics",
            description = "Returns analytics and statistics for the current user's personal goals."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GoalAnalyticsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/api/users/me/goals/analytics")
    public ResponseEntity<GoalAnalyticsResponse> getPersonalGoalAnalytics(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.ok(goalAnalyticsService.getPersonalAnalytics(userId));
    }

    @Operation(
            summary = "Get organization goal analytics",
            description = "Returns analytics and statistics for goals in a specific organization."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GoalAnalyticsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @GetMapping("/api/organizations/{orgId}/goals/analytics")
    public ResponseEntity<GoalAnalyticsResponse> getOrganizationGoalAnalytics(
            @Parameter(description = "Organization ID") @PathVariable UUID orgId,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.ok(goalAnalyticsService.getOrganizationAnalytics(orgId, userId));
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
