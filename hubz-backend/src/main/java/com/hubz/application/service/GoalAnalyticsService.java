package com.hubz.application.service;

import com.hubz.application.dto.response.GoalAnalyticsResponse;
import com.hubz.application.dto.response.GoalAnalyticsResponse.GoalCompletionData;
import com.hubz.application.dto.response.GoalAnalyticsResponse.GoalProgress;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalAnalyticsService {

    private final GoalRepositoryPort goalRepository;
    private final TaskRepositoryPort taskRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int RISK_DAYS_THRESHOLD = 14; // Goals with less than 14 days and low progress
    private static final double RISK_PROGRESS_THRESHOLD = 50.0; // Less than 50% progress

    public GoalAnalyticsResponse getPersonalAnalytics(UUID userId) {
        List<Goal> goals = goalRepository.findPersonalGoals(userId);
        return buildAnalytics(goals);
    }

    public GoalAnalyticsResponse getOrganizationAnalytics(UUID organizationId, UUID userId) {
        List<Goal> goals = goalRepository.findByOrganizationId(organizationId);
        return buildAnalytics(goals);
    }

    private GoalAnalyticsResponse buildAnalytics(List<Goal> goals) {
        if (goals.isEmpty()) {
            return buildEmptyAnalytics();
        }

        LocalDate today = LocalDate.now();

        // Calculate progress for each goal
        List<GoalProgress> goalProgressList = goals.stream()
                .map(goal -> calculateGoalProgress(goal, today))
                .toList();

        // Summary counts
        long totalGoals = goals.size();
        long completedGoals = goalProgressList.stream()
                .filter(gp -> gp.getProgressPercentage() >= 100)
                .count();
        long atRiskGoals = goalProgressList.stream()
                .filter(GoalProgress::isAtRisk)
                .count();
        long inProgressGoals = totalGoals - completedGoals;

        // Overall progress
        double overallProgress = goalProgressList.stream()
                .mapToDouble(GoalProgress::getProgressPercentage)
                .average()
                .orElse(0.0);
        overallProgress = Math.round(overallProgress * 100) / 100.0;

        double completionRate = totalGoals > 0
                ? Math.round((double) completedGoals / totalGoals * 100 * 100) / 100.0
                : 0.0;

        // Distribution by type
        Map<String, Long> goalsByType = goals.stream()
                .collect(Collectors.groupingBy(
                        g -> g.getType().name(),
                        Collectors.counting()
                ));

        Map<String, Double> avgProgressByType = goalProgressList.stream()
                .collect(Collectors.groupingBy(
                        GoalProgress::getType,
                        Collectors.averagingDouble(GoalProgress::getProgressPercentage)
                ));
        avgProgressByType.replaceAll((k, v) -> Math.round(v * 100) / 100.0);

        // Goals at risk
        List<GoalProgress> goalsAtRisk = goalProgressList.stream()
                .filter(GoalProgress::isAtRisk)
                .sorted(Comparator.comparingInt(GoalProgress::getDaysRemaining))
                .toList();

        // Track/behind schedule
        int goalsOnTrack = (int) goalProgressList.stream()
                .filter(GoalProgress::isOnTrack)
                .count();
        int goalsBehindSchedule = (int) goalProgressList.stream()
                .filter(gp -> !gp.isOnTrack() && gp.getProgressPercentage() < 100)
                .count();

        // Average velocity
        double averageVelocity = goalProgressList.stream()
                .mapToDouble(GoalProgress::getVelocityPerDay)
                .average()
                .orElse(0.0);
        averageVelocity = Math.round(averageVelocity * 100) / 100.0;

        // Historical completions (by month)
        List<GoalCompletionData> completionHistory = calculateCompletionHistory(goals);

        return GoalAnalyticsResponse.builder()
                .totalGoals(totalGoals)
                .completedGoals(completedGoals)
                .inProgressGoals(inProgressGoals)
                .atRiskGoals(atRiskGoals)
                .overallProgressPercentage(overallProgress)
                .goalCompletionRate(completionRate)
                .goalsByType(goalsByType)
                .avgProgressByType(avgProgressByType)
                .goalProgressList(goalProgressList)
                .goalsAtRisk(goalsAtRisk)
                .completionHistory(completionHistory)
                .goalsOnTrack(goalsOnTrack)
                .goalsBehindSchedule(goalsBehindSchedule)
                .averageVelocity(averageVelocity)
                .build();
    }

    private GoalProgress calculateGoalProgress(Goal goal, LocalDate today) {
        // Get tasks for this goal
        List<Task> tasks = taskRepository.findByGoalId(goal.getId());
        int totalTasks = tasks.size();
        int completedTasks = (int) tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();

        // Calculate progress percentage
        double progressPercentage = totalTasks > 0
                ? Math.round((double) completedTasks / totalTasks * 100 * 100) / 100.0
                : 0.0;

        // Calculate days
        int daysElapsed = (int) ChronoUnit.DAYS.between(
                goal.getCreatedAt().toLocalDate(), today);
        daysElapsed = Math.max(daysElapsed, 1); // At least 1 day

        int daysRemaining = goal.getDeadline() != null
                ? (int) ChronoUnit.DAYS.between(today, goal.getDeadline())
                : 365; // Default to 1 year if no deadline

        // Calculate velocity (tasks completed per day)
        double velocityPerDay = (double) completedTasks / daysElapsed;
        velocityPerDay = Math.round(velocityPerDay * 100) / 100.0;

        // Calculate required velocity to meet deadline
        int tasksRemaining = totalTasks - completedTasks;
        double requiredVelocity = daysRemaining > 0 && tasksRemaining > 0
                ? (double) tasksRemaining / daysRemaining
                : 0.0;
        requiredVelocity = Math.round(requiredVelocity * 100) / 100.0;

        // Predict completion date
        String predictedCompletionDate = null;
        if (velocityPerDay > 0 && tasksRemaining > 0) {
            int daysToComplete = (int) Math.ceil(tasksRemaining / velocityPerDay);
            LocalDate predictedDate = today.plusDays(daysToComplete);
            predictedCompletionDate = predictedDate.format(DATE_FORMATTER);
        } else if (progressPercentage >= 100) {
            predictedCompletionDate = "Completed";
        }

        // Determine if on track
        boolean isOnTrack = velocityPerDay >= requiredVelocity || progressPercentage >= 100;

        // Determine risk
        boolean isAtRisk = false;
        String riskReason = null;

        if (goal.getDeadline() != null && progressPercentage < 100) {
            // Check deadline proximity
            boolean deadlineClose = daysRemaining <= RISK_DAYS_THRESHOLD && daysRemaining >= 0;
            boolean lowProgress = progressPercentage < RISK_PROGRESS_THRESHOLD;
            boolean overdue = daysRemaining < 0;

            if (overdue) {
                isAtRisk = true;
                riskReason = "Deadline passed";
            } else if (deadlineClose && lowProgress) {
                isAtRisk = true;
                riskReason = String.format("Only %d days remaining with %.1f%% progress",
                        daysRemaining, progressPercentage);
            } else if (!isOnTrack && daysRemaining <= 30) {
                isAtRisk = true;
                riskReason = "Current velocity insufficient to meet deadline";
            }
        }

        return GoalProgress.builder()
                .goalId(goal.getId().toString())
                .title(goal.getTitle())
                .description(goal.getDescription())
                .type(goal.getType().name())
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .progressPercentage(progressPercentage)
                .deadline(goal.getDeadline() != null ? goal.getDeadline().format(DATE_FORMATTER) : null)
                .daysRemaining(daysRemaining)
                .daysElapsed(daysElapsed)
                .velocityPerDay(velocityPerDay)
                .requiredVelocity(requiredVelocity)
                .predictedCompletionDate(predictedCompletionDate)
                .isAtRisk(isAtRisk)
                .isOnTrack(isOnTrack)
                .riskReason(riskReason)
                .build();
    }

    private List<GoalCompletionData> calculateCompletionHistory(List<Goal> goals) {
        // Group completed goals by month (using updatedAt as completion date proxy)
        LocalDate today = LocalDate.now();
        LocalDate sixMonthsAgo = today.minusMonths(6);

        Map<String, Long> completionsByMonth = new LinkedHashMap<>();

        // Initialize last 6 months
        for (int i = 5; i >= 0; i--) {
            LocalDate month = today.minusMonths(i);
            String monthKey = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            completionsByMonth.put(monthKey, 0L);
        }

        // Count goals that were completed (100% task progress) based on updatedAt
        for (Goal goal : goals) {
            List<Task> tasks = taskRepository.findByGoalId(goal.getId());
            if (tasks.isEmpty()) continue;

            long completedTasks = tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE)
                    .count();

            if (completedTasks == tasks.size()) {
                // Goal is complete, use updatedAt as completion date
                if (goal.getUpdatedAt() != null) {
                    LocalDate completionDate = goal.getUpdatedAt().toLocalDate();
                    if (!completionDate.isBefore(sixMonthsAgo)) {
                        String monthKey = completionDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                        completionsByMonth.merge(monthKey, 1L, Long::sum);
                    }
                }
            }
        }

        return completionsByMonth.entrySet().stream()
                .map(entry -> GoalCompletionData.builder()
                        .month(entry.getKey())
                        .completedCount(entry.getValue())
                        .build())
                .toList();
    }

    private GoalAnalyticsResponse buildEmptyAnalytics() {
        return GoalAnalyticsResponse.builder()
                .totalGoals(0)
                .completedGoals(0)
                .inProgressGoals(0)
                .atRiskGoals(0)
                .overallProgressPercentage(0.0)
                .goalCompletionRate(0.0)
                .goalsByType(Map.of())
                .avgProgressByType(Map.of())
                .goalProgressList(List.of())
                .goalsAtRisk(List.of())
                .completionHistory(List.of())
                .goalsOnTrack(0)
                .goalsBehindSchedule(0)
                .averageVelocity(0.0)
                .build();
    }
}
