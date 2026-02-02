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
public class GoalAnalyticsResponse {

    // Summary
    private long totalGoals;
    private long completedGoals;
    private long inProgressGoals;
    private long atRiskGoals; // low progress + close deadline

    // Overall progress
    private double overallProgressPercentage;
    private double goalCompletionRate;

    // Distribution by type
    private Map<String, Long> goalsByType; // SHORT, MEDIUM, LONG
    private Map<String, Double> avgProgressByType;

    // Individual goal progress
    private List<GoalProgress> goalProgressList;

    // Goals at risk (low progress + deadline approaching)
    private List<GoalProgress> goalsAtRisk;

    // Historical completions
    private List<GoalCompletionData> completionHistory;

    // Prediction insights
    private int goalsOnTrack;
    private int goalsBehindSchedule;
    private double averageVelocity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalProgress {
        private String goalId;
        private String title;
        private String description;
        private String type;
        private int totalTasks;
        private int completedTasks;
        private double progressPercentage;
        private String deadline;
        private int daysRemaining;
        private int daysElapsed;
        private double velocityPerDay; // tasks completed per day since creation
        private double requiredVelocity; // tasks needed per day to meet deadline
        private String predictedCompletionDate;
        private boolean isAtRisk;
        private boolean isOnTrack;
        private String riskReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalCompletionData {
        private String month;
        private long completedCount;
    }
}
