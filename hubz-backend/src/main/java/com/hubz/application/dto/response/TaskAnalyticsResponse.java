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
public class TaskAnalyticsResponse {

    // Summary counts
    private long totalTasks;
    private long todoCount;
    private long inProgressCount;
    private long doneCount;

    // Completion metrics
    private double completionRate; // percentage of tasks completed
    private long overdueCount;
    private double overdueRate; // percentage of tasks overdue (among those with due date)

    // Time metrics (in hours)
    private Double averageCompletionTimeHours;
    private Double averageTimeInTodoHours;
    private Double averageTimeInProgressHours;

    // Distribution by priority
    private Map<String, Long> tasksByPriority;

    // Distribution by status
    private Map<String, Long> tasksByStatus;

    // Time series data for charts
    private List<TimeSeriesData> tasksCreatedOverTime;
    private List<TimeSeriesData> tasksCompletedOverTime;

    // Burndown/Burnup data
    private List<BurndownData> burndownChart;

    // Velocity data (tasks completed per week)
    private List<VelocityData> velocityChart;

    // Cumulative Flow Diagram data
    private List<CumulativeFlowData> cumulativeFlowDiagram;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesData {
        private String date;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BurndownData {
        private String date;
        private long remainingTasks;
        private long completedTasks;
        private long totalTasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VelocityData {
        private String weekStart;
        private long completedTasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CumulativeFlowData {
        private String date;
        private long todo;
        private long inProgress;
        private long done;
    }
}
