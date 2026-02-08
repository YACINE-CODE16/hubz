package com.hubz.presentation.controller;

import com.hubz.application.dto.request.AnalyticsFilterRequest;
import com.hubz.application.dto.response.*;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.AnalyticsService;
import com.hubz.application.service.CalendarAnalyticsService;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CalendarAnalyticsService calendarAnalyticsService;
    private final UserRepositoryPort userRepositoryPort;

    /**
     * Get task analytics for an organization.
     * Includes completion rates, burndown charts, velocity, and more.
     * Supports optional dynamic filters: date range, member IDs, statuses, priorities.
     */
    @GetMapping("/organizations/{orgId}/analytics/tasks")
    public ResponseEntity<TaskAnalyticsResponse> getTaskAnalytics(
            @PathVariable UUID orgId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<UUID> memberIds,
            @RequestParam(required = false) List<TaskStatus> statuses,
            @RequestParam(required = false) List<TaskPriority> priorities,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        AnalyticsFilterRequest filters = buildFilters(startDate, endDate, memberIds, statuses, priorities);
        return ResponseEntity.ok(analyticsService.getTaskAnalytics(orgId, currentUserId, filters));
    }

    /**
     * Get member analytics for an organization.
     * Includes productivity rankings, workload distribution, and activity heatmaps.
     * Supports optional dynamic filters: date range, member IDs, statuses, priorities.
     */
    @GetMapping("/organizations/{orgId}/analytics/members")
    public ResponseEntity<MemberAnalyticsResponse> getMemberAnalytics(
            @PathVariable UUID orgId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<UUID> memberIds,
            @RequestParam(required = false) List<TaskStatus> statuses,
            @RequestParam(required = false) List<TaskPriority> priorities,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        AnalyticsFilterRequest filters = buildFilters(startDate, endDate, memberIds, statuses, priorities);
        return ResponseEntity.ok(analyticsService.getMemberAnalytics(orgId, currentUserId, filters));
    }

    /**
     * Get goal analytics for an organization.
     * Includes progress tracking, at-risk goals, and predictions.
     * Supports optional dynamic filters: date range, member IDs, statuses, priorities.
     */
    @GetMapping("/organizations/{orgId}/analytics/goals")
    public ResponseEntity<GoalAnalyticsResponse> getGoalAnalytics(
            @PathVariable UUID orgId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<UUID> memberIds,
            @RequestParam(required = false) List<TaskStatus> statuses,
            @RequestParam(required = false) List<TaskPriority> priorities,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        AnalyticsFilterRequest filters = buildFilters(startDate, endDate, memberIds, statuses, priorities);
        return ResponseEntity.ok(analyticsService.getGoalAnalytics(orgId, currentUserId, filters));
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

    /**
     * Get calendar and time analytics for the current user.
     * Includes events per period, time distribution, occupancy rate, busiest days,
     * time slot distribution, meeting vs work ratio, conflicts, and availability score.
     *
     * @param startDate Start of the analysis period (default: 30 days ago)
     * @param endDate   End of the analysis period (default: today)
     */
    @GetMapping("/users/me/calendar-analytics")
    public ResponseEntity<CalendarAnalyticsResponse> getCalendarAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);

        // Default to last 30 days if not specified
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);

        return ResponseEntity.ok(calendarAnalyticsService.getCalendarAnalytics(userId, start, end));
    }

    /**
     * Build an AnalyticsFilterRequest from individual request parameters.
     * Returns null if no filter parameters are provided.
     */
    private AnalyticsFilterRequest buildFilters(
            LocalDate startDate,
            LocalDate endDate,
            List<UUID> memberIds,
            List<TaskStatus> statuses,
            List<TaskPriority> priorities) {

        AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .memberIds(memberIds)
                .statuses(statuses)
                .priorities(priorities)
                .build();

        return filters.hasAnyFilter() ? filters : null;
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
