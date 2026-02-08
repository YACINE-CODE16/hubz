package com.hubz.application.service;

import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.HabitLogRepositoryPort;
import com.hubz.application.port.out.HabitRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.model.Event;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.Habit;
import com.hubz.domain.model.HabitLog;
import com.hubz.domain.model.Task;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

/**
 * Service for generating and sending weekly digest emails to users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyDigestService {

    private final UserRepositoryPort userRepository;
    private final TaskRepositoryPort taskRepository;
    private final GoalRepositoryPort goalRepository;
    private final HabitRepositoryPort habitRepository;
    private final HabitLogRepositoryPort habitLogRepository;
    private final EventRepositoryPort eventRepository;
    private final EmailService emailService;

    /**
     * Data class to hold weekly digest statistics.
     */
    public record WeeklyDigestData(
            int tasksCompletedThisWeek,
            int tasksCompletedLastWeek,
            int goalsInProgress,
            int goalsCompleted,
            int habitsCompletionRate,
            int upcomingEventsCount,
            String topAchievement
    ) {}

    /**
     * Generate digest data for a user.
     *
     * @param userId the user ID
     * @return WeeklyDigestData containing all statistics
     */
    public WeeklyDigestData generateDigest(UUID userId) {
        LocalDate today = LocalDate.now();

        // Calculate week boundaries
        LocalDate thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = thisWeekStart.minusDays(1);
        LocalDate nextWeekEnd = thisWeekStart.plusWeeks(1).plusDays(6);

        // Get tasks for this user
        List<Task> userTasks = taskRepository.findByAssigneeId(userId);

        // Count tasks completed this week vs last week
        int tasksCompletedThisWeek = countTasksCompletedInPeriod(userTasks, thisWeekStart, today);
        int tasksCompletedLastWeek = countTasksCompletedInPeriod(userTasks, lastWeekStart, lastWeekEnd);

        // Get goals for this user (personal goals) and count by completion status
        List<Goal> personalGoals = goalRepository.findPersonalGoals(userId);
        int goalsInProgress = 0;
        int goalsCompleted = 0;

        for (Goal goal : personalGoals) {
            List<Task> goalTasks = taskRepository.findByGoalId(goal.getId());
            int totalTasks = goalTasks.size();
            int completedTasks = (int) goalTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE)
                    .count();

            if (totalTasks > 0 && completedTasks == totalTasks) {
                goalsCompleted++;
            } else {
                goalsInProgress++;
            }
        }

        // Calculate habits completion rate for this week
        int habitsCompletionRate = calculateHabitsCompletionRate(userId, thisWeekStart, today);

        // Count upcoming events for next week
        LocalDateTime nextWeekStartTime = thisWeekStart.plusWeeks(1).atStartOfDay();
        LocalDateTime nextWeekEndTime = nextWeekEnd.atTime(23, 59, 59);
        List<Event> upcomingEvents = eventRepository.findPersonalEventsByTimeRange(
                userId, nextWeekStartTime, nextWeekEndTime);
        int upcomingEventsCount = upcomingEvents.size();

        // Generate top achievement
        String topAchievement = generateTopAchievement(
                tasksCompletedThisWeek,
                tasksCompletedLastWeek,
                goalsCompleted,
                habitsCompletionRate
        );

        return new WeeklyDigestData(
                tasksCompletedThisWeek,
                tasksCompletedLastWeek,
                goalsInProgress,
                goalsCompleted,
                habitsCompletionRate,
                upcomingEventsCount,
                topAchievement
        );
    }

    /**
     * Send weekly digest email to a user.
     *
     * @param userId the user ID
     */
    public void sendWeeklyDigest(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Cannot send weekly digest: user {} not found", userId);
            return;
        }

        try {
            WeeklyDigestData digest = generateDigest(userId);

            emailService.sendWeeklyDigestEmail(
                    user.getEmail(),
                    user.getFirstName(),
                    digest.tasksCompletedThisWeek(),
                    digest.tasksCompletedLastWeek(),
                    digest.goalsInProgress(),
                    digest.goalsCompleted(),
                    digest.habitsCompletionRate(),
                    digest.upcomingEventsCount(),
                    digest.topAchievement()
            );

            log.info("Weekly digest sent to user {} ({})", userId, user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send weekly digest to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Count tasks completed within a date range.
     */
    private int countTasksCompletedInPeriod(List<Task> tasks, LocalDate startDate, LocalDate endDate) {
        return (int) tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .filter(task -> task.getUpdatedAt() != null)
                .filter(task -> {
                    LocalDate updatedDate = task.getUpdatedAt().toLocalDate();
                    return !updatedDate.isBefore(startDate) && !updatedDate.isAfter(endDate);
                })
                .count();
    }

    /**
     * Calculate habits completion rate for a period.
     * Returns percentage (0-100).
     */
    private int calculateHabitsCompletionRate(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<Habit> habits = habitRepository.findByUserId(userId);
        if (habits.isEmpty()) {
            return 100; // No habits = 100% completion
        }

        List<UUID> habitIds = habits.stream().map(Habit::getId).toList();
        List<HabitLog> logs = habitLogRepository.findByHabitIdInAndDateRange(habitIds, startDate, endDate);

        // Count completed logs
        long completedLogs = logs.stream()
                .filter(log -> Boolean.TRUE.equals(log.getCompleted()))
                .count();

        // Calculate expected completions (habits * days in period)
        long daysInPeriod = startDate.until(endDate).getDays() + 1;
        long expectedCompletions = habits.size() * daysInPeriod;

        if (expectedCompletions == 0) {
            return 100;
        }

        return (int) ((completedLogs * 100) / expectedCompletions);
    }

    /**
     * Generate a top achievement message based on user performance.
     */
    private String generateTopAchievement(
            int tasksThisWeek,
            int tasksLastWeek,
            int goalsCompleted,
            int habitsCompletionRate
    ) {
        // Prioritize achievements
        if (goalsCompleted > 0) {
            return goalsCompleted == 1
                    ? "Objectif atteint cette semaine !"
                    : goalsCompleted + " objectifs atteints cette semaine !";
        }

        if (tasksThisWeek > tasksLastWeek && tasksThisWeek > 5) {
            int improvement = tasksThisWeek - tasksLastWeek;
            return "+" + improvement + " taches completees par rapport a la semaine derniere !";
        }

        if (habitsCompletionRate >= 90) {
            return "Excellent taux de completion des habitudes : " + habitsCompletionRate + "% !";
        }

        if (tasksThisWeek >= 10) {
            return tasksThisWeek + " taches completees cette semaine - Bravo !";
        }

        if (habitsCompletionRate >= 70) {
            return "Bonne constance dans vos habitudes : " + habitsCompletionRate + "% !";
        }

        // No special achievement
        return null;
    }
}
