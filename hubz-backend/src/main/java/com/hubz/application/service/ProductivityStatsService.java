package com.hubz.application.service;

import com.hubz.application.dto.response.ProductivityStatsResponse;
import com.hubz.application.dto.response.ProductivityStatsResponse.DailyTaskCount;
import com.hubz.application.port.out.ProductivityStatsRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for calculating and providing productivity statistics for a user.
 * Part of the application layer - uses port interfaces for data access.
 */
@Service
@RequiredArgsConstructor
public class ProductivityStatsService {

    private final ProductivityStatsRepositoryPort productivityStatsRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Map<String, String> DAY_TRANSLATIONS = Map.of(
            "Monday", "Lundi",
            "Tuesday", "Mardi",
            "Wednesday", "Mercredi",
            "Thursday", "Jeudi",
            "Friday", "Vendredi",
            "Saturday", "Samedi",
            "Sunday", "Dimanche"
    );

    /**
     * Get comprehensive productivity statistics for a user.
     */
    public ProductivityStatsResponse getProductivityStats(UUID userId) {
        LocalDate today = LocalDate.now();

        // Define time ranges
        LocalDateTime thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime thisWeekEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDateTime lastWeekEnd = thisWeekStart;

        LocalDateTime thisMonthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime thisMonthEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime lastMonthStart = thisMonthStart.minusMonths(1);
        LocalDateTime lastMonthEnd = thisMonthStart;

        // Get 30-day range for charts
        LocalDateTime thirtyDaysAgo = today.minusDays(30).atStartOfDay();

        // Calculate this week stats
        int tasksCompletedThisWeek = productivityStatsRepository.countCompletedTasksByUserInRange(userId, thisWeekStart, thisWeekEnd);
        int totalTasksThisWeek = productivityStatsRepository.countTotalTasksByUserInRange(userId, thisWeekStart, thisWeekEnd);

        // Calculate this month stats
        int tasksCompletedThisMonth = productivityStatsRepository.countCompletedTasksByUserInRange(userId, thisMonthStart, thisMonthEnd);
        int totalTasksThisMonth = productivityStatsRepository.countTotalTasksByUserInRange(userId, thisMonthStart, thisMonthEnd);

        // Calculate last week/month for comparison
        int tasksCompletedLastWeek = productivityStatsRepository.countCompletedTasksByUserInRange(userId, lastWeekStart, lastWeekEnd);
        int tasksCompletedLastMonth = productivityStatsRepository.countCompletedTasksByUserInRange(userId, lastMonthStart, lastMonthEnd);

        // Calculate completion rates
        double weeklyCompletionRate = totalTasksThisWeek > 0 ? (double) tasksCompletedThisWeek / totalTasksThisWeek * 100 : 0;
        double monthlyCompletionRate = totalTasksThisMonth > 0 ? (double) tasksCompletedThisMonth / totalTasksThisMonth * 100 : 0;

        // Calculate weekly/monthly change
        double weeklyChange = calculatePercentageChange(tasksCompletedLastWeek, tasksCompletedThisWeek);
        double monthlyChange = calculatePercentageChange(tasksCompletedLastMonth, tasksCompletedThisMonth);

        // Get average completion time (last 30 days)
        Double avgCompletionTime = productivityStatsRepository.getAverageCompletionTimeHours(userId, thirtyDaysAgo, thisWeekEnd);

        // Get daily completion counts for chart (last 30 days)
        List<DailyTaskCount> dailyTasksCompleted = getDailyTaskCounts(userId, thirtyDaysAgo, thisWeekEnd);

        // Get most productive day
        String mostProductiveDayEnglish = productivityStatsRepository.getMostProductiveDay(userId, thirtyDaysAgo, thisWeekEnd);
        String mostProductiveDay = translateDayName(mostProductiveDayEnglish);

        // Get completion by priority (this month)
        int[] priorityCounts = productivityStatsRepository.countCompletedByPriority(userId, thisMonthStart, thisMonthEnd);

        // Calculate productive streak
        List<LocalDateTime> productiveDates = productivityStatsRepository.getProductiveDates(userId, today.minusYears(1).atStartOfDay(), thisWeekEnd);
        int[] streaks = calculateStreaks(productiveDates, today);
        int productiveStreak = streaks[0];
        int longestProductiveStreak = streaks[1];

        // Calculate productivity score (0-100)
        int productivityScore = calculateProductivityScore(
                weeklyCompletionRate,
                monthlyCompletionRate,
                productiveStreak,
                tasksCompletedThisWeek,
                priorityCounts
        );

        // Generate insight message
        String insight = generateInsight(weeklyChange, monthlyChange, productiveStreak, tasksCompletedThisWeek);

        return ProductivityStatsResponse.builder()
                .tasksCompletedThisWeek(tasksCompletedThisWeek)
                .tasksCompletedThisMonth(tasksCompletedThisMonth)
                .totalTasksThisWeek(totalTasksThisWeek)
                .totalTasksThisMonth(totalTasksThisMonth)
                .weeklyCompletionRate(Math.round(weeklyCompletionRate * 10) / 10.0)
                .monthlyCompletionRate(Math.round(monthlyCompletionRate * 10) / 10.0)
                .averageCompletionTimeHours(avgCompletionTime != null ? Math.round(avgCompletionTime * 10) / 10.0 : null)
                .productiveStreak(productiveStreak)
                .longestProductiveStreak(longestProductiveStreak)
                .weeklyChange(Math.round(weeklyChange * 10) / 10.0)
                .monthlyChange(Math.round(monthlyChange * 10) / 10.0)
                .insight(insight)
                .productivityScore(productivityScore)
                .dailyTasksCompleted(dailyTasksCompleted)
                .mostProductiveDay(mostProductiveDay)
                .urgentTasksCompleted(priorityCounts[0])
                .highPriorityTasksCompleted(priorityCounts[1])
                .mediumPriorityTasksCompleted(priorityCounts[2])
                .lowPriorityTasksCompleted(priorityCounts[3])
                .build();
    }

    private double calculatePercentageChange(int previous, int current) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((double) (current - previous) / previous) * 100;
    }

    private List<DailyTaskCount> getDailyTaskCounts(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> rawCounts = productivityStatsRepository.getDailyCompletionCounts(userId, startDate, endDate);

        // Create a map of date to count
        Map<String, Integer> countsByDate = new HashMap<>();
        for (Object[] row : rawCounts) {
            String date;
            if (row[0] instanceof java.sql.Date) {
                date = ((java.sql.Date) row[0]).toLocalDate().format(DATE_FORMATTER);
            } else if (row[0] instanceof LocalDate) {
                date = ((LocalDate) row[0]).format(DATE_FORMATTER);
            } else {
                date = row[0].toString();
            }
            int count = ((Number) row[1]).intValue();
            countsByDate.put(date, count);
        }

        // Fill in all days in the range with 0 for missing days
        List<DailyTaskCount> result = new ArrayList<>();
        LocalDate current = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();

        while (!current.isAfter(end)) {
            String dateStr = current.format(DATE_FORMATTER);
            int count = countsByDate.getOrDefault(dateStr, 0);
            result.add(DailyTaskCount.builder()
                    .date(dateStr)
                    .count(count)
                    .build());
            current = current.plusDays(1);
        }

        return result;
    }

    private String translateDayName(String englishDay) {
        if (englishDay == null) {
            return null;
        }
        return DAY_TRANSLATIONS.getOrDefault(englishDay, englishDay);
    }

    /**
     * Calculate current and longest productive streaks.
     * Returns [currentStreak, longestStreak].
     */
    private int[] calculateStreaks(List<LocalDateTime> productiveDates, LocalDate today) {
        if (productiveDates.isEmpty()) {
            return new int[]{0, 0};
        }

        // Convert to LocalDate set for faster lookup
        java.util.Set<LocalDate> productiveDaysSet = new java.util.HashSet<>();
        for (LocalDateTime dt : productiveDates) {
            productiveDaysSet.add(dt.toLocalDate());
        }

        // Calculate current streak (must include today or yesterday)
        int currentStreak = 0;
        LocalDate checkDate = today;

        // If today is not productive, start from yesterday
        if (!productiveDaysSet.contains(today)) {
            checkDate = today.minusDays(1);
        }

        while (productiveDaysSet.contains(checkDate)) {
            currentStreak++;
            checkDate = checkDate.minusDays(1);
        }

        // Calculate longest streak
        int longestStreak = 0;
        int tempStreak = 0;
        LocalDate prevDate = null;

        List<LocalDate> sortedDates = productiveDates.stream()
                .map(LocalDateTime::toLocalDate)
                .distinct()
                .sorted()
                .toList();

        for (LocalDate date : sortedDates) {
            if (prevDate == null || date.equals(prevDate.plusDays(1))) {
                tempStreak++;
            } else {
                longestStreak = Math.max(longestStreak, tempStreak);
                tempStreak = 1;
            }
            prevDate = date;
        }
        longestStreak = Math.max(longestStreak, tempStreak);

        return new int[]{currentStreak, longestStreak};
    }

    /**
     * Calculate a productivity score from 0 to 100.
     */
    private int calculateProductivityScore(
            double weeklyCompletionRate,
            double monthlyCompletionRate,
            int productiveStreak,
            int tasksCompletedThisWeek,
            int[] priorityCounts
    ) {
        // Weights for different factors
        double score = 0;

        // Completion rate contributes 30%
        score += (weeklyCompletionRate * 0.3);

        // Monthly rate contributes 20%
        score += (monthlyCompletionRate * 0.2);

        // Streak contributes 20% (max contribution at 14 days)
        score += Math.min(productiveStreak, 14) / 14.0 * 20;

        // Tasks completed this week contributes 20% (max contribution at 10 tasks)
        score += Math.min(tasksCompletedThisWeek, 10) / 10.0 * 20;

        // Priority bonus: completing urgent/high priority tasks adds 10%
        int highPriorityCompleted = priorityCounts[0] + priorityCounts[1];
        score += Math.min(highPriorityCompleted, 5) / 5.0 * 10;

        return (int) Math.min(Math.round(score), 100);
    }

    /**
     * Generate a contextual insight message based on the user's productivity.
     */
    private String generateInsight(double weeklyChange, double monthlyChange, int productiveStreak, int tasksCompletedThisWeek) {
        List<String> insights = new ArrayList<>();

        // Weekly change insight
        if (weeklyChange > 20) {
            insights.add(String.format("Vous avez complete %.0f%% de taches de plus que la semaine derniere !", weeklyChange));
        } else if (weeklyChange < -20) {
            insights.add(String.format("Vous avez complete %.0f%% de taches de moins que la semaine derniere.", Math.abs(weeklyChange)));
        }

        // Streak insight
        if (productiveStreak >= 7) {
            insights.add(String.format("Impressionnant ! Vous etes sur une serie de %d jours productifs.", productiveStreak));
        } else if (productiveStreak >= 3) {
            insights.add(String.format("Beau travail ! %d jours productifs consecutifs.", productiveStreak));
        }

        // Weekly tasks insight
        if (tasksCompletedThisWeek >= 10) {
            insights.add("Excellente semaine avec " + tasksCompletedThisWeek + " taches completees !");
        } else if (tasksCompletedThisWeek == 0) {
            insights.add("Commencez la semaine du bon pied en completant votre premiere tache !");
        }

        // Monthly trend
        if (monthlyChange > 0 && monthlyChange <= 20) {
            insights.add("Votre productivite mensuelle est en hausse. Continuez ainsi !");
        }

        if (insights.isEmpty()) {
            return "Continuez votre excellent travail !";
        }

        return insights.get(0);
    }
}
