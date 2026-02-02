package com.hubz.presentation.controller;

import com.hubz.application.dto.response.*;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.AnalyticsService;
import com.hubz.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserRepositoryPort userRepositoryPort;

    /**
     * Get task analytics for an organization.
     * Includes completion rates, burndown charts, velocity, and more.
     */
    @GetMapping("/organizations/{orgId}/analytics/tasks")
    public ResponseEntity<TaskAnalyticsResponse> getTaskAnalytics(
            @PathVariable UUID orgId,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(analyticsService.getTaskAnalytics(orgId, currentUserId));
    }

    /**
     * Get member analytics for an organization.
     * Includes productivity rankings, workload distribution, and activity heatmaps.
     */
    @GetMapping("/organizations/{orgId}/analytics/members")
    public ResponseEntity<MemberAnalyticsResponse> getMemberAnalytics(
            @PathVariable UUID orgId,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(analyticsService.getMemberAnalytics(orgId, currentUserId));
    }

    /**
     * Get goal analytics for an organization.
     * Includes progress tracking, at-risk goals, and predictions.
     */
    @GetMapping("/organizations/{orgId}/analytics/goals")
    public ResponseEntity<GoalAnalyticsResponse> getGoalAnalytics(
            @PathVariable UUID orgId,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(analyticsService.getGoalAnalytics(orgId, currentUserId));
    }

    /**
     * Get overall organization analytics.
     * Includes health score, trends, and monthly growth.
     */
    @GetMapping("/organizations/{orgId}/analytics")
    public ResponseEntity<OrganizationAnalyticsResponse> getOrganizationAnalytics(
            @PathVariable UUID orgId,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(analyticsService.getOrganizationAnalytics(orgId, currentUserId));
    }

    /**
     * Get habit analytics for the current user.
     * Includes streaks, completion rates, and heatmaps.
     */
    @GetMapping("/users/me/analytics/habits")
    public ResponseEntity<HabitAnalyticsResponse> getHabitAnalytics(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.ok(analyticsService.getHabitAnalytics(userId));
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
