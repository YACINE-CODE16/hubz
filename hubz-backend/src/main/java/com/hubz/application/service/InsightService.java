package com.hubz.application.service;

import com.hubz.application.dto.response.InsightResponse;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.HabitLogRepositoryPort;
import com.hubz.application.port.out.HabitRepositoryPort;
import com.hubz.application.port.out.ProductivityStatsRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.InsightType;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.Habit;
import com.hubz.domain.model.HabitLog;
import com.hubz.domain.model.Insight;
import com.hubz.domain.model.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for generating personal insights and recommendations for users.
 * Analyzes task completion patterns, habit streaks, goal progress, and workload.
 */
@Service
@RequiredArgsConstructor
public class InsightService {

    private final TaskRepositoryPort taskRepository;
    private final HabitRepositoryPort habitRepository;
    private final HabitLogRepositoryPort habitLogRepository;
    private final GoalRepositoryPort goalRepository;
    private final ProductivityStatsRepositoryPort productivityStatsRepository;

    private static final Map<DayOfWeek, String> DAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "Lundi",
            DayOfWeek.TUESDAY, "Mardi",
            DayOfWeek.WEDNESDAY, "Mercredi",
            DayOfWeek.THURSDAY, "Jeudi",
            DayOfWeek.FRIDAY, "Vendredi",
            DayOfWeek.SATURDAY, "Samedi",
            DayOfWeek.SUNDAY, "Dimanche"
    );

    /**
     * Generate a list of insights for a user.
     *
     * @param userId the user ID
     * @return list of insights sorted by priority (highest first)
     */
    public List<InsightResponse> generateInsights(UUID userId) {
        List<Insight> insights = new ArrayList<>();

        // Generate insights from different sources
        insights.addAll(generateTaskPatternInsights(userId));
        insights.addAll(generateHabitInsights(userId));
        insights.addAll(generateGoalInsights(userId));
        insights.addAll(generateWorkloadInsights(userId));
        insights.addAll(generateCelebrationInsights(userId));
        insights.addAll(generateProductivityPatternInsights(userId));

        // Sort by priority (highest first), then by creation date (newest first)
        insights.sort(Comparator
                .comparingInt(Insight::getPriority).reversed()
                .thenComparing(Comparator.comparing(Insight::getCreatedAt).reversed()));

        // Convert to response DTOs (limit to top 10 insights)
        return insights.stream()
                .limit(10)
                .map(this::toResponse)
                .toList();
    }

    /**
     * Generate insights based on task completion patterns.
     */
    List<Insight> generateTaskPatternInsights(UUID userId) {
        List<Insight> insights = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDateTime thirtyDaysAgo = today.minusDays(30).atStartOfDay();
        LocalDateTime now = today.plusDays(1).atStartOfDay();

        // Get most productive day
        String mostProductiveDay = productivityStatsRepository.getMostProductiveDay(userId, thirtyDaysAgo, now);
        if (mostProductiveDay != null) {
            String dayNameFr = translateDayName(mostProductiveDay);
            insights.add(Insight.builder()
                    .id(UUID.randomUUID())
                    .type(InsightType.PATTERN_DETECTED)
                    .title("Jour le plus productif")
                    .message(String.format("Vous completez le plus de taches le %s. Planifiez vos taches importantes ce jour-la !", dayNameFr))
                    .priority(2)
                    .actionable(true)
                    .actionUrl("/personal/tasks")
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return insights;
    }

    /**
     * Generate insights based on habit tracking.
     */
    List<Insight> generateHabitInsights(UUID userId) {
        List<Insight> insights = new ArrayList<>();
        List<Habit> habits = habitRepository.findByUserId(userId);

        if (habits.isEmpty()) {
            return insights;
        }

        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(7);

        List<UUID> habitIds = habits.stream().map(Habit::getId).toList();
        List<HabitLog> recentLogs = habitLogRepository.findByHabitIdInAndDateRange(habitIds, sevenDaysAgo, today);

        // Calculate streaks for each habit
        for (Habit habit : habits) {
            int currentStreak = calculateHabitStreak(habit.getId(), today);

            // Celebration for habit streaks
            if (currentStreak == 7) {
                insights.add(Insight.builder()
                        .id(UUID.randomUUID())
                        .type(InsightType.CELEBRATION)
                        .title("Serie de 7 jours !")
                        .message(String.format("Felicitations ! Vous etes sur une serie de 7 jours pour \"%s\" !", habit.getName()))
                        .priority(4)
                        .actionable(true)
                        .actionUrl("/personal/habits")
                        .createdAt(LocalDateTime.now())
                        .build());
            } else if (currentStreak == 30) {
                insights.add(Insight.builder()
                        .id(UUID.randomUUID())
                        .type(InsightType.CELEBRATION)
                        .title("Serie de 30 jours !")
                        .message(String.format("Incroyable ! 30 jours consecutifs pour \"%s\" ! Vous etes un champion !", habit.getName()))
                        .priority(5)
                        .actionable(true)
                        .actionUrl("/personal/habits")
                        .createdAt(LocalDateTime.now())
                        .build());
            } else if (currentStreak >= 3 && currentStreak < 7) {
                insights.add(Insight.builder()
                        .id(UUID.randomUUID())
                        .type(InsightType.HABIT_SUGGESTION)
                        .title("Continuez votre serie !")
                        .message(String.format("%d jours consecutifs pour \"%s\" ! Encore quelques jours pour atteindre une semaine complete.", currentStreak, habit.getName()))
                        .priority(3)
                        .actionable(true)
                        .actionUrl("/personal/habits")
                        .createdAt(LocalDateTime.now())
                        .build());
            }

            // Check if habit is at risk (no completion in last 2 days but had a streak)
            boolean completedYesterday = isHabitCompletedOnDate(habit.getId(), recentLogs, today.minusDays(1));
            boolean completedDayBefore = isHabitCompletedOnDate(habit.getId(), recentLogs, today.minusDays(2));
            boolean completedToday = isHabitCompletedOnDate(habit.getId(), recentLogs, today);

            if (!completedToday && !completedYesterday && completedDayBefore) {
                insights.add(Insight.builder()
                        .id(UUID.randomUUID())
                        .type(InsightType.HABIT_SUGGESTION)
                        .title("Habitude a risque")
                        .message(String.format("Vous n'avez pas complete \"%s\" depuis 2 jours. Ne laissez pas votre serie s'interrompre !", habit.getName()))
                        .priority(3)
                        .actionable(true)
                        .actionUrl("/personal/habits")
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        }

        // Calculate overall habit completion rate
        long totalExpected = habits.size() * 7L; // 7 days
        long totalCompleted = recentLogs.stream().filter(HabitLog::getCompleted).count();

        if (totalExpected > 0) {
            double completionRate = (double) totalCompleted / totalExpected * 100;

            if (completionRate >= 90) {
                insights.add(Insight.builder()
                        .id(UUID.randomUUID())
                        .type(InsightType.CELEBRATION)
                        .title("Excellent suivi des habitudes !")
                        .message(String.format("%.0f%% de vos habitudes completees cette semaine ! Continuez comme ca !", completionRate))
                        .priority(4)
                        .actionable(false)
                        .actionUrl(null)
                        .createdAt(LocalDateTime.now())
                        .build());
            } else if (completionRate < 50 && completionRate > 0) {
                insights.add(Insight.builder()
                        .id(UUID.randomUUID())
                        .type(InsightType.HABIT_SUGGESTION)
                        .title("Habitudes a ameliorer")
                        .message("Votre taux de completion des habitudes est en dessous de 50% cette semaine. Essayez de vous concentrer sur une habitude a la fois.")
                        .priority(3)
                        .actionable(true)
                        .actionUrl("/personal/habits")
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        }

        return insights;
    }

    /**
     * Generate insights based on goal progress.
     */
    List<Insight> generateGoalInsights(UUID userId) {
        List<Insight> insights = new ArrayList<>();
        List<Goal> personalGoals = goalRepository.findPersonalGoals(userId);
        LocalDate today = LocalDate.now();

        for (Goal goal : personalGoals) {
            if (goal.getDeadline() == null) {
                continue;
            }

            // Get tasks linked to this goal
            List<Task> goalTasks = taskRepository.findByGoalId(goal.getId());
            long totalTasks = goalTasks.size();
            long completedTasks = goalTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE)
                    .count();

            double progress = totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0;
            long daysUntilDeadline = ChronoUnit.DAYS.between(today, goal.getDeadline());

            // Goal at risk: low progress with deadline approaching
            if (daysUntilDeadline <= 7 && daysUntilDeadline > 0 && progress < 50) {
                insights.add(Insight.builder()
                        .id(UUID.randomUUID())
                        .type(InsightType.GOAL_ALERT)
                        .title("Objectif a risque")
                        .message(String.format("\"%s\" est a %.0f%% avec seulement %d jour(s) restant(s). Concentrez vos efforts !", goal.getTitle(), progress, daysUntilDeadline))
                        .priority(5)
                        .actionable(true)
                        .actionUrl("/personal/goals")
                        .createdAt(LocalDateTime.now())
                        .build());
            } else if (daysUntilDeadline <= 3 && daysUntilDeadline > 0 && progress < 80) {
                insights.add(Insight.builder()
                        .id(UUID.randomUUID())
                        .type(InsightType.GOAL_ALERT)
                        .title("Echeance proche !")
                        .message(String.format("Plus que %d jour(s) pour \"%s\". Vous etes a %.0f%% de progression.", daysUntilDeadline, goal.getTitle(), progress))
                        .priority(4)
                        .actionable(true)
                        .actionUrl("/personal/goals")
                        .createdAt(LocalDateTime.now())
                        .build());
            }

            // Goal almost completed
            if (progress >= 90 && progress < 100 && totalTasks > 0) {
                insights.add(Insight.builder()
                        .id(UUID.randomUUID())
                        .type(InsightType.PRODUCTIVITY_TIP)
                        .title("Presque termine !")
                        .message(String.format("\"%s\" est a %.0f%% ! Plus que quelques taches pour atteindre votre objectif.", goal.getTitle(), progress))
                        .priority(3)
                        .actionable(true)
                        .actionUrl("/personal/goals")
                        .createdAt(LocalDateTime.now())
                        .build());
            }

            // Goal completed celebration
            if (progress == 100 && totalTasks > 0) {
                insights.add(Insight.builder()
                        .id(UUID.randomUUID())
                        .type(InsightType.CELEBRATION)
                        .title("Objectif atteint !")
                        .message(String.format("Bravo ! Vous avez complete toutes les taches de \"%s\" !", goal.getTitle()))
                        .priority(4)
                        .actionable(true)
                        .actionUrl("/personal/goals")
                        .createdAt(LocalDateTime.now())
                        .build());
            }

            // Overdue goal
            if (daysUntilDeadline < 0 && progress < 100) {
                insights.add(Insight.builder()
                        .id(UUID.randomUUID())
                        .type(InsightType.GOAL_ALERT)
                        .title("Objectif en retard")
                        .message(String.format("\"%s\" a depasse son echeance de %d jour(s). Envisagez de reprogrammer ou de diviser en sous-objectifs.", goal.getTitle(), Math.abs(daysUntilDeadline)))
                        .priority(4)
                        .actionable(true)
                        .actionUrl("/personal/goals")
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        }

        return insights;
    }

    /**
     * Generate insights based on workload.
     */
    List<Insight> generateWorkloadInsights(UUID userId) {
        List<Insight> insights = new ArrayList<>();
        List<Task> userTasks = taskRepository.findByAssigneeId(userId);
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // Count tasks due this week that are not done
        long tasksDueThisWeek = userTasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .filter(t -> t.getDueDate() != null)
                .filter(t -> {
                    LocalDate dueDate = t.getDueDate().toLocalDate();
                    return !dueDate.isBefore(today) && !dueDate.isAfter(weekEnd);
                })
                .count();

        if (tasksDueThisWeek >= 15) {
            insights.add(Insight.builder()
                    .id(UUID.randomUUID())
                    .type(InsightType.WORKLOAD_WARNING)
                    .title("Charge de travail elevee")
                    .message(String.format("Vous avez %d taches prevues cette semaine. Envisagez de prioriser ou de deleguer certaines taches.", tasksDueThisWeek))
                    .priority(4)
                    .actionable(true)
                    .actionUrl("/personal/tasks")
                    .createdAt(LocalDateTime.now())
                    .build());
        } else if (tasksDueThisWeek >= 10) {
            insights.add(Insight.builder()
                    .id(UUID.randomUUID())
                    .type(InsightType.WORKLOAD_WARNING)
                    .title("Semaine chargee")
                    .message(String.format("%d taches sont prevues cette semaine. Organisez bien votre temps !", tasksDueThisWeek))
                    .priority(3)
                    .actionable(true)
                    .actionUrl("/personal/tasks")
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        // Count overdue tasks
        long overdueTasks = userTasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .filter(t -> t.getDueDate() != null)
                .filter(t -> t.getDueDate().toLocalDate().isBefore(today))
                .count();

        if (overdueTasks > 5) {
            insights.add(Insight.builder()
                    .id(UUID.randomUUID())
                    .type(InsightType.WORKLOAD_WARNING)
                    .title("Taches en retard")
                    .message(String.format("Vous avez %d taches en retard. Prenez un moment pour les traiter ou les reprogrammer.", overdueTasks))
                    .priority(4)
                    .actionable(true)
                    .actionUrl("/personal/tasks")
                    .createdAt(LocalDateTime.now())
                    .build());
        } else if (overdueTasks > 0) {
            insights.add(Insight.builder()
                    .id(UUID.randomUUID())
                    .type(InsightType.WORKLOAD_WARNING)
                    .title("Tache(s) en retard")
                    .message(String.format("Vous avez %d tache(s) en retard. N'oubliez pas de les traiter !", overdueTasks))
                    .priority(3)
                    .actionable(true)
                    .actionUrl("/personal/tasks")
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return insights;
    }

    /**
     * Generate celebration insights based on achievements.
     */
    List<Insight> generateCelebrationInsights(UUID userId) {
        List<Insight> insights = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDateTime thisMonthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = today.plusDays(1).atStartOfDay();

        // Count tasks completed this month
        int tasksCompletedThisMonth = productivityStatsRepository.countCompletedTasksByUserInRange(userId, thisMonthStart, now);

        // Milestones: 10, 25, 50, 100 tasks
        if (tasksCompletedThisMonth >= 100) {
            insights.add(Insight.builder()
                    .id(UUID.randomUUID())
                    .type(InsightType.CELEBRATION)
                    .title("100 taches ce mois !")
                    .message("Extraordinaire ! Vous avez complete 100 taches ce mois. Vous etes une machine de productivite !")
                    .priority(5)
                    .actionable(false)
                    .actionUrl(null)
                    .createdAt(LocalDateTime.now())
                    .build());
        } else if (tasksCompletedThisMonth >= 50) {
            insights.add(Insight.builder()
                    .id(UUID.randomUUID())
                    .type(InsightType.CELEBRATION)
                    .title("50 taches ce mois !")
                    .message("Felicitations ! Vous avez complete 50 taches ce mois. Excellent travail !")
                    .priority(4)
                    .actionable(false)
                    .actionUrl(null)
                    .createdAt(LocalDateTime.now())
                    .build());
        } else if (tasksCompletedThisMonth >= 25) {
            insights.add(Insight.builder()
                    .id(UUID.randomUUID())
                    .type(InsightType.CELEBRATION)
                    .title("25 taches ce mois !")
                    .message("Bravo ! 25 taches completees ce mois. Continuez sur cette lancee !")
                    .priority(3)
                    .actionable(false)
                    .actionUrl(null)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return insights;
    }

    /**
     * Generate insights based on productivity patterns.
     */
    List<Insight> generateProductivityPatternInsights(UUID userId) {
        List<Insight> insights = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDateTime thirtyDaysAgo = today.minusDays(30).atStartOfDay();
        LocalDateTime now = today.plusDays(1).atStartOfDay();

        // Get daily completion counts
        List<Object[]> dailyCounts = productivityStatsRepository.getDailyCompletionCounts(userId, thirtyDaysAgo, now);

        if (dailyCounts.isEmpty()) {
            return insights;
        }

        // Analyze which day of week is most productive
        Map<DayOfWeek, Integer> dayOfWeekCounts = new HashMap<>();
        for (Object[] row : dailyCounts) {
            LocalDate date;
            if (row[0] instanceof java.sql.Date) {
                date = ((java.sql.Date) row[0]).toLocalDate();
            } else if (row[0] instanceof LocalDate) {
                date = (LocalDate) row[0];
            } else {
                continue;
            }
            int count = ((Number) row[1]).intValue();
            dayOfWeekCounts.merge(date.getDayOfWeek(), count, Integer::sum);
        }

        if (!dayOfWeekCounts.isEmpty()) {
            DayOfWeek bestDay = dayOfWeekCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            DayOfWeek worstDay = dayOfWeekCounts.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (bestDay != null && worstDay != null && !bestDay.equals(worstDay)) {
                int bestCount = dayOfWeekCounts.get(bestDay);
                int worstCount = dayOfWeekCounts.get(worstDay);

                if (bestCount > worstCount * 2 && bestCount >= 5) {
                    insights.add(Insight.builder()
                            .id(UUID.randomUUID())
                            .type(InsightType.PRODUCTIVITY_TIP)
                            .title("Conseil de productivite")
                            .message(String.format("Vous etes %dx plus productif le %s que le %s. Utilisez cette information pour planifier vos taches importantes !",
                                    (int) Math.ceil((double) bestCount / Math.max(worstCount, 1)),
                                    DAY_NAMES.get(bestDay),
                                    DAY_NAMES.get(worstDay)))
                            .priority(2)
                            .actionable(false)
                            .actionUrl(null)
                            .createdAt(LocalDateTime.now())
                            .build());
                }
            }
        }

        // Check for consistent daily activity
        long daysWithActivity = dailyCounts.stream()
                .filter(row -> ((Number) row[1]).intValue() > 0)
                .count();

        if (daysWithActivity >= 20) {
            insights.add(Insight.builder()
                    .id(UUID.randomUUID())
                    .type(InsightType.CELEBRATION)
                    .title("Constance remarquable !")
                    .message(String.format("Vous avez ete actif %d jours sur les 30 derniers. Votre regularite est impressionnante !", daysWithActivity))
                    .priority(3)
                    .actionable(false)
                    .actionUrl(null)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return insights;
    }

    /**
     * Calculate the current streak for a habit.
     */
    int calculateHabitStreak(UUID habitId, LocalDate today) {
        List<HabitLog> logs = habitLogRepository.findByHabitId(habitId);

        if (logs.isEmpty()) {
            return 0;
        }

        // Create a set of completed dates
        var completedDates = logs.stream()
                .filter(HabitLog::getCompleted)
                .map(HabitLog::getDate)
                .collect(Collectors.toSet());

        // Calculate streak starting from today or yesterday
        int streak = 0;
        LocalDate checkDate = today;

        // If not completed today, start from yesterday
        if (!completedDates.contains(today)) {
            checkDate = today.minusDays(1);
        }

        while (completedDates.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }

    /**
     * Check if a habit was completed on a specific date.
     */
    private boolean isHabitCompletedOnDate(UUID habitId, List<HabitLog> logs, LocalDate date) {
        return logs.stream()
                .filter(log -> log.getHabitId().equals(habitId))
                .filter(log -> log.getDate().equals(date))
                .anyMatch(HabitLog::getCompleted);
    }

    /**
     * Translate English day name to French.
     */
    private String translateDayName(String englishDay) {
        return switch (englishDay) {
            case "Monday" -> "Lundi";
            case "Tuesday" -> "Mardi";
            case "Wednesday" -> "Mercredi";
            case "Thursday" -> "Jeudi";
            case "Friday" -> "Vendredi";
            case "Saturday" -> "Samedi";
            case "Sunday" -> "Dimanche";
            default -> englishDay;
        };
    }

    /**
     * Convert domain Insight to response DTO.
     */
    private InsightResponse toResponse(Insight insight) {
        return InsightResponse.builder()
                .id(insight.getId())
                .type(insight.getType())
                .title(insight.getTitle())
                .message(insight.getMessage())
                .priority(insight.getPriority())
                .actionable(insight.isActionable())
                .actionUrl(insight.getActionUrl())
                .createdAt(insight.getCreatedAt())
                .build();
    }
}
