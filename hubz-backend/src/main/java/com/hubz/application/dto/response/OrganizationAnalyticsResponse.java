package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationAnalyticsResponse {

    // Organization health score (0-100)
    private int healthScore;

    // Overview counts
    private long totalMembers;
    private long activeMembers; // active in last 7 days
    private long totalTasks;
    private long activeTasks;
    private long totalGoals;
    private long totalEvents;
    private long totalNotes;
    private long totalTeams;

    // Activity metrics
    private long tasksCreatedThisWeek;
    private long tasksCompletedThisWeek;
    private long eventsThisWeek;

    // Trends
    private double taskCompletionTrend; // % change from last week
    private double memberActivityTrend; // % change from last week

    // Activity timeline
    private List<ActivityTimelineEntry> recentActivity;

    // Team performance
    private List<TeamPerformance> teamPerformance;

    // Monthly growth
    private List<MonthlyGrowth> monthlyGrowth;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityTimelineEntry {
        private String timestamp;
        private String type; // TASK_CREATED, TASK_COMPLETED, MEMBER_JOINED, etc.
        private String description;
        private String actorName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamPerformance {
        private String teamId;
        private String teamName;
        private long memberCount;
        private long tasksCompleted;
        private long activeTasks;
        private double completionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyGrowth {
        private String month;
        private long tasksCreated;
        private long tasksCompleted;
        private long newMembers;
        private long goalsCompleted;
    }
}
