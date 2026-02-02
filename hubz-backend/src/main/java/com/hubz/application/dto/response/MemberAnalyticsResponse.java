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
}
