package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberAnalyticsResponse {

    // Member productivity rankings
    private List<MemberProductivity> memberProductivity;

    // Workload distribution
    private List<MemberWorkload> memberWorkload;

    // Top performers
    private List<MemberProductivity> topPerformers;

    // Members with overdue tasks or high workload
    private List<MemberWorkload> overloadedMembers;

    // Activity heatmap data (contributions per day)
    private List<ActivityData> activityHeatmap;

    // Average completion time per member (sorted fastest to slowest)
    private List<MemberCompletionTime> memberCompletionTimes;

    // Inactive members (no activity in X days)
    private List<InactiveMember> inactiveMembers;

    // Team performance comparison
    private List<TeamPerformanceComparison> teamPerformanceComparison;

    // Member workload heatmap (members x days of week)
    private List<MemberWorkloadHeatmapEntry> memberWorkloadHeatmap;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberProductivity {
        private String memberId;
        private String memberName;
        private String memberEmail;
        private long tasksCompleted;
        private long totalTasksAssigned;
        private double completionRate;
        private Double averageCompletionTimeHours;
        private double productivityScore; // weighted by priority
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberWorkload {
        private String memberId;
        private String memberName;
        private String memberEmail;
        private long activeTasks; // TODO + IN_PROGRESS
        private long todoTasks;
        private long inProgressTasks;
        private long overdueTasks;
        private boolean isOverloaded; // flag if too many tasks
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityData {
        private String date;
        private String memberId;
        private String memberName;
        private long activityCount; // tasks created or completed
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberCompletionTime {
        private String memberId;
        private String memberName;
        private String memberEmail;
        private Double averageCompletionTimeHours;
        private long tasksCompleted;
        private int rank;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InactiveMember {
        private String memberId;
        private String memberName;
        private String memberEmail;
        private String lastActivityDate; // ISO date of last activity, null if never active
        private long inactiveDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamPerformanceComparison {
        private String teamId;
        private String teamName;
        private int memberCount;
        private long totalTasksCompleted;
        private double avgTasksCompletedPerMember;
        private Double avgCompletionTimeHours;
        private double teamVelocity; // tasks completed per week (last 4 weeks)
        private double completionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberWorkloadHeatmapEntry {
        private String memberId;
        private String memberName;
        private Map<String, Long> tasksByDayOfWeek; // MONDAY -> count, TUESDAY -> count, etc.
    }
}
