package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateGoalRequest;
import com.hubz.application.dto.request.UpdateGoalRequest;
import com.hubz.application.dto.response.GoalAnalyticsResponse;
import com.hubz.application.dto.response.GoalResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.GoalAnalyticsService;
import com.hubz.application.service.GoalService;
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
public class GoalController {

    private final GoalService goalService;
    private final GoalAnalyticsService goalAnalyticsService;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping("/api/organizations/{orgId}/goals")
    public ResponseEntity<List<GoalResponse>> getByOrganization(
            @PathVariable UUID orgId, Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(goalService.getByOrganization(orgId, currentUserId));
    }

    @PostMapping("/api/organizations/{orgId}/goals")
    public ResponseEntity<GoalResponse> createOrganizationGoal(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateGoalRequest request,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalService.create(request, orgId, userId));
    }

    @GetMapping("/api/users/me/goals")
    public ResponseEntity<List<GoalResponse>> getPersonalGoals(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.ok(goalService.getPersonalGoals(userId));
    }

    @PostMapping("/api/users/me/goals")
    public ResponseEntity<GoalResponse> createPersonalGoal(
            @Valid @RequestBody CreateGoalRequest request,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalService.create(request, null, userId));
    }

    @PutMapping("/api/goals/{id}")
    public ResponseEntity<GoalResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGoalRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(goalService.update(id, request, currentUserId));
    }

    @DeleteMapping("/api/goals/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        goalService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/users/me/goals/analytics")
    public ResponseEntity<GoalAnalyticsResponse> getPersonalGoalAnalytics(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.ok(goalAnalyticsService.getPersonalAnalytics(userId));
    }

    @GetMapping("/api/organizations/{orgId}/goals/analytics")
    public ResponseEntity<GoalAnalyticsResponse> getOrganizationGoalAnalytics(
            @PathVariable UUID orgId,
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
