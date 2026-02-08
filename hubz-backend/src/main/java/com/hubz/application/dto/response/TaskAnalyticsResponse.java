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
    private List<BurnupData> burnupChart;

    // Velocity data (tasks completed per week)
    private List<VelocityData> velocityChart;

    // Cumulative Flow Diagram data
    private List<CumulativeFlowData> cumulativeFlowDiagram;

    // Throughput chart data (tasks completed per day with rolling average)
    private List<ThroughputData> throughputChart;

    // Cycle time distribution (histogram buckets)
    private List<CycleTimeBucket> cycleTimeDistribution;

    // Lead time trend over time
    private List<LeadTimeData> leadTimeTrend;
    private Double averageLeadTimeHours;

    // Work in Progress (WIP) chart
    private List<WIPData> wipChart;
    private Double averageWIP;

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BurnupData {
        private String date;
        private long cumulativeCompleted; // Total tasks completed up to this date
        private long totalScope;           // Total tasks in scope (including new tasks added)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThroughputData {
        private String date;
        private long completedCount;       // Tasks completed on this day
        private Double rollingAverage;     // 7-day rolling average
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CycleTimeBucket {
        private String bucket;             // e.g., "<1 day", "1-3 days", "3-7 days", etc.
        private int minHours;              // Minimum hours for this bucket
        private int maxHours;              // Maximum hours for this bucket (-1 for unlimited)
        private long count;                // Number of tasks in this bucket
        private double percentage;         // Percentage of total completed tasks
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeadTimeData {
        private String date;               // Week start date
        private Double averageLeadTimeHours; // Average lead time for tasks completed that week
        private long taskCount;            // Number of tasks completed that week
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WIPData {
        private String date;
        private long wipCount;             // Number of tasks in IN_PROGRESS status
        private long todoCount;            // Number of tasks in TODO status
        private long totalActive;          // Total active tasks (TODO + IN_PROGRESS)
    }
}
