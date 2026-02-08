package com.hubz.presentation.controller;

import com.hubz.application.dto.response.ActivityHeatmapResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.ActivityHeatmapService;
import com.hubz.application.service.AuthorizationService;
import com.hubz.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for activity heatmap endpoints.
 * Provides contribution heatmap data for users and organizations.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActivityHeatmapController {

    private final ActivityHeatmapService activityHeatmapService;
    private final AuthorizationService authorizationService;
    private final UserRepositoryPort userRepositoryPort;

    /**
     * Get activity heatmap for the authenticated user.
     * Returns 12 months of contribution data in a GitHub-style format.
     *
     * @param authentication The authenticated user's details
     * @return ActivityHeatmapResponse with daily activity data and statistics
     */
    @GetMapping("/users/me/activity-heatmap")
    public ResponseEntity<ActivityHeatmapResponse> getUserActivityHeatmap(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        ActivityHeatmapResponse heatmap = activityHeatmapService.getUserActivityHeatmap(userId);
        return ResponseEntity.ok(heatmap);
    }

    /**
     * Get aggregated activity heatmap for an organization.
     * Shows combined contributions from all organization members.
     *
     * @param orgId          The organization ID
     * @param authentication The authenticated user's details
     * @return ActivityHeatmapResponse with aggregated team activity
     */
    @GetMapping("/organizations/{orgId}/activity-heatmap")
    public ResponseEntity<ActivityHeatmapResponse> getOrganizationActivityHeatmap(
            @PathVariable UUID orgId,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        authorizationService.checkOrganizationAccess(orgId, userId);
        ActivityHeatmapResponse heatmap = activityHeatmapService.getTeamActivityHeatmap(orgId);
        return ResponseEntity.ok(heatmap);
    }

    /**
     * Get activity heatmap for a specific member within an organization.
     * Shows the member's contributions within the organization context.
     *
     * @param orgId          The organization ID
     * @param memberId       The member's user ID
     * @param authentication The authenticated user's details
     * @return ActivityHeatmapResponse with member's activity data
     */
    @GetMapping("/organizations/{orgId}/members/{memberId}/activity-heatmap")
    public ResponseEntity<ActivityHeatmapResponse> getMemberActivityHeatmap(
            @PathVariable UUID orgId,
            @PathVariable UUID memberId,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        authorizationService.checkOrganizationAccess(orgId, userId);
        ActivityHeatmapResponse heatmap = activityHeatmapService.getMemberActivityHeatmap(orgId, memberId);
        return ResponseEntity.ok(heatmap);
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
