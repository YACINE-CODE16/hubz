package com.hubz.application.service;

import com.hubz.application.dto.response.*;
import com.hubz.application.dto.response.TaskAnalyticsResponse.*;
import com.hubz.application.dto.response.MemberAnalyticsResponse.*;
import com.hubz.application.dto.response.GoalAnalyticsResponse.*;
import com.hubz.application.dto.response.HabitAnalyticsResponse.*;
import com.hubz.application.dto.response.OrganizationAnalyticsResponse.*;
import com.hubz.application.port.out.*;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TaskRepositoryPort taskRepository;
    private final GoalRepositoryPort goalRepository;
    private final HabitRepositoryPort habitRepository;
    private final HabitLogRepositoryPort habitLogRepository;
    private final OrganizationMemberRepositoryPort memberRepository;
    private final UserRepositoryPort userRepository;
    private final AuthorizationService authorizationService;

    private static final int OVERLOAD_THRESHOLD = 10; // tasks considered as overloaded
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==================== TASK ANALYTICS ====================

    public TaskAnalyticsResponse getTaskAnalytics(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        List<Task> tasks = taskRepository.findByOrganizationId(organizationId);
        LocalDate today = LocalDate.now();

        // Basic counts
        long totalTasks = tasks.size();
        long todoCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
        long inProgressCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long doneCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();

        // Completion rate
        double completionRate = totalTasks > 0 ? (double) doneCount / totalTasks * 100 : 0;

        // Overdue tasks (tasks with due date in the past that are not done)
        long overdueCount = tasks.stream()
                .filter(t -> t.getDueDate() != null
                        && t.getDueDate().toLocalDate().isBefore(today)
                        && t.getStatus() != TaskStatus.DONE)
                .count();

        long tasksWithDueDate = tasks.stream().filter(t -> t.getDueDate() != null).count();
        double overdueRate = tasksWithDueDate > 0 ? (double) overdueCount / tasksWithDueDate * 100 : 0;

        // Average completion time (for completed tasks)
        Double avgCompletionTimeHours = calculateAverageCompletionTime(tasks);

        // Distribution by priority
        Map<String, Long> tasksByPriority = tasks.stream()
                .filter(t -> t.getPriority() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getPriority().name(),
                        Collectors.counting()
                ));

        // Distribution by status
        Map<String, Long> tasksByStatus = Map.of(
                "TODO", todoCount,
                "IN_PROGRESS", inProgressCount,
                "DONE", doneCount
        );

        // Time series data (last 30 days)
        List<TimeSeriesData> tasksCreatedOverTime = getTasksCreatedOverTime(tasks, 30);
        List<TimeSeriesData> tasksCompletedOverTime = getTasksCompletedOverTime(tasks, 30);

        // Burndown chart data
        List<BurndownData> burndownChart = calculateBurndownChart(tasks, 30);

        // Velocity chart (last 12 weeks)
        List<VelocityData> velocityChart = calculateVelocityChart(tasks, 12);

        // Cumulative Flow Diagram
        List<CumulativeFlowData> cumulativeFlowDiagram = calculateCumulativeFlow(tasks, 30);

        return TaskAnalyticsResponse.builder()
                .totalTasks(totalTasks)
                .todoCount(todoCount)
                .inProgressCount(inProgressCount)
                .doneCount(doneCount)
                .completionRate(Math.round(completionRate * 100.0) / 100.0)
                .overdueCount(overdueCount)
                .overdueRate(Math.round(overdueRate * 100.0) / 100.0)
                .averageCompletionTimeHours(avgCompletionTimeHours)
                .tasksByPriority(tasksByPriority)
                .tasksByStatus(tasksByStatus)
                .tasksCreatedOverTime(tasksCreatedOverTime)
                .tasksCompletedOverTime(tasksCompletedOverTime)
                .burndownChart(burndownChart)
                .velocityChart(velocityChart)
                .cumulativeFlowDiagram(cumulativeFlowDiagram)
                .build();
    }

    private Double calculateAverageCompletionTime(List<Task> tasks) {
        List<Task> completedTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE && t.getCreatedAt() != null && t.getUpdatedAt() != null)
                .toList();

        if (completedTasks.isEmpty()) {
            return null;
        }

        double totalHours = completedTasks.stream()
                .mapToDouble(t -> ChronoUnit.HOURS.between(t.getCreatedAt(), t.getUpdatedAt()))
                .sum();

        return Math.round(totalHours / completedTasks.size() * 100.0) / 100.0;
    }

    private List<TimeSeriesData> getTasksCreatedOverTime(List<Task> tasks, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        Map<LocalDate, Long> countByDate = tasks.stream()
                .filter(t -> t.getCreatedAt() != null)
                .filter(t -> !t.getCreatedAt().toLocalDate().isBefore(startDate))
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        List<TimeSeriesData> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            result.add(TimeSeriesData.builder()
                    .date(date.format(DATE_FORMATTER))
                    .count(countByDate.getOrDefault(date, 0L))
                    .build());
        }
        return result;
    }

    private List<TimeSeriesData> getTasksCompletedOverTime(List<Task> tasks, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        Map<LocalDate, Long> countByDate = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE && t.getUpdatedAt() != null)
                .filter(t -> !t.getUpdatedAt().toLocalDate().isBefore(startDate))
                .collect(Collectors.groupingBy(
                        t -> t.getUpdatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        List<TimeSeriesData> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            result.add(TimeSeriesData.builder()
                    .date(date.format(DATE_FORMATTER))
                    .count(countByDate.getOrDefault(date, 0L))
                    .build());
        }
        return result;
    }

    private List<BurndownData> calculateBurndownChart(List<Task> tasks, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        long totalTasksAtStart = tasks.stream()
                .filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().toLocalDate().isAfter(startDate))
                .count();

        List<BurndownData> result = new ArrayList<>();
        long cumulative = totalTasksAtStart;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date;

            long createdOnDate = tasks.stream()
                    .filter(t -> t.getCreatedAt() != null
                            && t.getCreatedAt().toLocalDate().equals(currentDate))
                    .count();

            long completedOnDate = tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE
                            && t.getUpdatedAt() != null
                            && t.getUpdatedAt().toLocalDate().equals(currentDate))
                    .count();

            cumulative = cumulative + createdOnDate;
            long remaining = cumulative - tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE
                            && t.getUpdatedAt() != null
                            && !t.getUpdatedAt().toLocalDate().isAfter(currentDate))
                    .count();

            result.add(BurndownData.builder()
                    .date(currentDate.format(DATE_FORMATTER))
                    .remainingTasks(remaining)
                    .completedTasks(completedOnDate)
                    .totalTasks(cumulative)
                    .build());
        }
        return result;
    }

    private List<VelocityData> calculateVelocityChart(List<Task> tasks, int weeks) {
        LocalDate endDate = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        List<VelocityData> result = new ArrayList<>();

        for (int i = weeks - 1; i >= 0; i--) {
            LocalDate weekStart = endDate.minusWeeks(i).with(weekFields.dayOfWeek(), 1);
            LocalDate weekEnd = weekStart.plusDays(6);

            long completed = tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE && t.getUpdatedAt() != null)
                    .filter(t -> {
                        LocalDate completedDate = t.getUpdatedAt().toLocalDate();
                        return !completedDate.isBefore(weekStart) && !completedDate.isAfter(weekEnd);
                    })
                    .count();

            result.add(VelocityData.builder()
                    .weekStart(weekStart.format(DATE_FORMATTER))
                    .completedTasks(completed)
                    .build());
        }
        return result;
    }

    private List<CumulativeFlowData> calculateCumulativeFlow(List<Task> tasks, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        List<CumulativeFlowData> result = new ArrayList<>();

        // For simplicity, we calculate cumulative based on current state
        // In a real implementation, you'd need task state history
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date;

            // Simple approximation - distribute based on current ratios
            long todoCount = tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.TODO
                            && t.getCreatedAt() != null
                            && !t.getCreatedAt().toLocalDate().isAfter(currentDate))
                    .count();

            long inProgressCount = tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS
                            && t.getCreatedAt() != null
                            && !t.getCreatedAt().toLocalDate().isAfter(currentDate))
                    .count();

            long doneCount = tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE
                            && t.getUpdatedAt() != null
                            && !t.getUpdatedAt().toLocalDate().isAfter(currentDate))
                    .count();

            result.add(CumulativeFlowData.builder()
                    .date(currentDate.format(DATE_FORMATTER))
                    .todo(todoCount)
                    .inProgress(inProgressCount)
                    .done(doneCount)
                    .build());
        }
        return result;
    }

    // ==================== MEMBER ANALYTICS ====================

    public MemberAnalyticsResponse getMemberAnalytics(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        List<OrganizationMember> members = memberRepository.findByOrganizationId(organizationId);
        List<Task> tasks = taskRepository.findByOrganizationId(organizationId);
        LocalDate today = LocalDate.now();

        // Build member productivity list
        List<MemberProductivity> memberProductivity = new ArrayList<>();
        List<MemberWorkload> memberWorkload = new ArrayList<>();

        for (OrganizationMember member : members) {
            User user = userRepository.findById(member.getUserId()).orElse(null);
            if (user == null) continue;

            String memberName = user.getFirstName() + " " + user.getLastName();
            String memberEmail = user.getEmail();

            // Tasks for this member
            List<Task> memberTasks = tasks.stream()
                    .filter(t -> member.getUserId().equals(t.getAssigneeId()))
                    .toList();

            long completed = memberTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE)
                    .count();

            long total = memberTasks.size();
            double completionRate = total > 0 ? (double) completed / total * 100 : 0;

            // Calculate productivity score (weighted by priority)
            double productivityScore = memberTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE)
                    .mapToDouble(t -> getPriorityWeight(t.getPriority()))
                    .sum();

            memberProductivity.add(MemberProductivity.builder()
                    .memberId(member.getUserId().toString())
                    .memberName(memberName)
                    .memberEmail(memberEmail)
                    .tasksCompleted(completed)
                    .totalTasksAssigned(total)
                    .completionRate(Math.round(completionRate * 100.0) / 100.0)
                    .productivityScore(productivityScore)
                    .build());

            // Workload
            long activeTasks = memberTasks.stream()
                    .filter(t -> t.getStatus() != TaskStatus.DONE)
                    .count();

            long todoTasks = memberTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.TODO)
                    .count();

            long inProgressTasks = memberTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                    .count();

            long overdueTasks = memberTasks.stream()
                    .filter(t -> t.getDueDate() != null
                            && t.getDueDate().toLocalDate().isBefore(today)
                            && t.getStatus() != TaskStatus.DONE)
                    .count();

            memberWorkload.add(MemberWorkload.builder()
                    .memberId(member.getUserId().toString())
                    .memberName(memberName)
                    .memberEmail(memberEmail)
                    .activeTasks(activeTasks)
                    .todoTasks(todoTasks)
                    .inProgressTasks(inProgressTasks)
                    .overdueTasks(overdueTasks)
                    .isOverloaded(activeTasks >= OVERLOAD_THRESHOLD)
                    .build());
        }

        // Sort by productivity score descending
        memberProductivity.sort((a, b) -> Double.compare(b.getProductivityScore(), a.getProductivityScore()));

        // Top 5 performers
        List<MemberProductivity> topPerformers = memberProductivity.stream()
                .limit(5)
                .toList();

        // Overloaded members
        List<MemberWorkload> overloadedMembers = memberWorkload.stream()
                .filter(MemberWorkload::isOverloaded)
                .toList();

        // Activity heatmap (last 30 days)
        List<ActivityData> activityHeatmap = calculateActivityHeatmap(tasks, members, 30);

        return MemberAnalyticsResponse.builder()
                .memberProductivity(memberProductivity)
                .memberWorkload(memberWorkload)
                .topPerformers(topPerformers)
                .overloadedMembers(overloadedMembers)
                .activityHeatmap(activityHeatmap)
                .build();
    }

    private double getPriorityWeight(TaskPriority priority) {
        if (priority == null) return 1.0;
        return switch (priority) {
            case LOW -> 1.0;
            case MEDIUM -> 2.0;
            case HIGH -> 3.0;
            case URGENT -> 4.0;
        };
    }

    private List<ActivityData> calculateActivityHeatmap(List<Task> tasks, List<OrganizationMember> members, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        List<ActivityData> result = new ArrayList<>();

        for (OrganizationMember member : members) {
            User user = userRepository.findById(member.getUserId()).orElse(null);
            if (user == null) continue;

            String memberName = user.getFirstName() + " " + user.getLastName();

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                final LocalDate currentDate = date;

                long activity = tasks.stream()
                        .filter(t -> member.getUserId().equals(t.getCreatorId()) || member.getUserId().equals(t.getAssigneeId()))
                        .filter(t -> {
                            boolean created = t.getCreatedAt() != null && t.getCreatedAt().toLocalDate().equals(currentDate);
                            boolean completed = t.getStatus() == TaskStatus.DONE && t.getUpdatedAt() != null
                                    && t.getUpdatedAt().toLocalDate().equals(currentDate);
                            return created || completed;
                        })
                        .count();

                if (activity > 0) {
                    result.add(ActivityData.builder()
                            .date(currentDate.format(DATE_FORMATTER))
                            .memberId(member.getUserId().toString())
                            .memberName(memberName)
                            .activityCount(activity)
                            .build());
                }
            }
        }

        return result;
    }

    // ==================== GOAL ANALYTICS ====================

    public GoalAnalyticsResponse getGoalAnalytics(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        List<Goal> goals = goalRepository.findByOrganizationId(organizationId);
        List<Task> tasks = taskRepository.findByOrganizationId(organizationId);
        LocalDate today = LocalDate.now();

        // Calculate goal progress based on linked tasks
        List<GoalProgress> goalProgressList = new ArrayList<>();
        long completedGoals = 0;
        long inProgressGoals = 0;
        long atRiskGoals = 0;
        double totalProgress = 0;

        for (Goal goal : goals) {
            // Get tasks linked to this goal
            List<Task> goalTasks = tasks.stream()
                    .filter(t -> goal.getId().equals(t.getGoalId()))
                    .toList();

            int totalTaskCount = goalTasks.size();
            int completedTaskCount = (int) goalTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE)
                    .count();

            double progressPercentage = totalTaskCount > 0
                    ? (double) completedTaskCount / totalTaskCount * 100
                    : 0;

            int daysRemaining = goal.getDeadline() != null
                    ? (int) ChronoUnit.DAYS.between(today, goal.getDeadline())
                    : Integer.MAX_VALUE;

            // Determine if at risk (less than 50% progress with less than 7 days remaining)
            boolean isAtRisk = progressPercentage < 50 && daysRemaining > 0 && daysRemaining <= 7;

            // Calculate velocity and predictions
            int daysElapsed = goal.getCreatedAt() != null
                    ? (int) ChronoUnit.DAYS.between(goal.getCreatedAt().toLocalDate(), today)
                    : 0;

            double velocityPerDay = 0;
            double requiredVelocity = 0;
            String predictedCompletionDate = null;
            boolean isOnTrack = true;
            String riskReason = null;

            if (daysElapsed > 0 && completedTaskCount > 0) {
                velocityPerDay = (double) completedTaskCount / daysElapsed;
            }

            int remainingTasks = totalTaskCount - completedTaskCount;
            if (daysRemaining > 0 && remainingTasks > 0) {
                requiredVelocity = (double) remainingTasks / daysRemaining;
                isOnTrack = velocityPerDay >= requiredVelocity || remainingTasks == 0;

                if (velocityPerDay > 0) {
                    long daysToComplete = (long) Math.ceil(remainingTasks / velocityPerDay);
                    predictedCompletionDate = today.plusDays(daysToComplete).format(DATE_FORMATTER);
                }
            }

            // Determine risk reason
            if (isAtRisk) {
                if (daysRemaining <= 3 && progressPercentage < 80) {
                    riskReason = "Critical: deadline imminent with low progress";
                } else if (requiredVelocity > velocityPerDay * 2) {
                    riskReason = "Velocity too low to meet deadline";
                } else {
                    riskReason = "Progress behind schedule";
                }
            }

            GoalProgress gp = GoalProgress.builder()
                    .goalId(goal.getId().toString())
                    .title(goal.getTitle())
                    .description(goal.getDescription())
                    .type(goal.getType() != null ? goal.getType().name() : null)
                    .totalTasks(totalTaskCount)
                    .completedTasks(completedTaskCount)
                    .progressPercentage(Math.round(progressPercentage * 100.0) / 100.0)
                    .deadline(goal.getDeadline() != null ? goal.getDeadline().format(DATE_FORMATTER) : null)
                    .daysRemaining(daysRemaining)
                    .daysElapsed(daysElapsed)
                    .velocityPerDay(Math.round(velocityPerDay * 100.0) / 100.0)
                    .requiredVelocity(Math.round(requiredVelocity * 100.0) / 100.0)
                    .predictedCompletionDate(predictedCompletionDate)
                    .isAtRisk(isAtRisk)
                    .isOnTrack(isOnTrack)
                    .riskReason(riskReason)
                    .build();

            goalProgressList.add(gp);
            totalProgress += progressPercentage;

            if (progressPercentage >= 100) {
                completedGoals++;
            } else {
                inProgressGoals++;
            }

            if (isAtRisk) {
                atRiskGoals++;
            }
        }

        // Goals at risk
        List<GoalProgress> goalsAtRisk = goalProgressList.stream()
                .filter(GoalProgress::isAtRisk)
                .toList();

        // Distribution by type
        Map<String, Long> goalsByType = goals.stream()
                .filter(g -> g.getType() != null)
                .collect(Collectors.groupingBy(
                        g -> g.getType().name(),
                        Collectors.counting()
                ));

        // Average progress by type
        Map<String, Double> avgProgressByType = goalProgressList.stream()
                .filter(g -> g.getType() != null)
                .collect(Collectors.groupingBy(
                        GoalProgress::getType,
                        Collectors.averagingDouble(GoalProgress::getProgressPercentage)
                ));

        // Overall metrics
        double overallProgressPercentage = goals.isEmpty() ? 0 : totalProgress / goals.size();
        double goalCompletionRate = goals.isEmpty() ? 0 : (double) completedGoals / goals.size() * 100;

        // Count goals on track vs behind schedule
        int goalsOnTrack = (int) goalProgressList.stream().filter(GoalProgress::isOnTrack).count();
        int goalsBehindSchedule = (int) goalProgressList.stream().filter(g -> !g.isOnTrack()).count();

        // Calculate average velocity
        double averageVelocity = goalProgressList.stream()
                .mapToDouble(GoalProgress::getVelocityPerDay)
                .average()
                .orElse(0.0);

        return GoalAnalyticsResponse.builder()
                .totalGoals(goals.size())
                .completedGoals(completedGoals)
                .inProgressGoals(inProgressGoals)
                .atRiskGoals(atRiskGoals)
                .overallProgressPercentage(Math.round(overallProgressPercentage * 100.0) / 100.0)
                .goalCompletionRate(Math.round(goalCompletionRate * 100.0) / 100.0)
                .goalsByType(goalsByType)
                .avgProgressByType(avgProgressByType)
                .goalProgressList(goalProgressList)
                .goalsAtRisk(goalsAtRisk)
                .goalsOnTrack(goalsOnTrack)
                .goalsBehindSchedule(goalsBehindSchedule)
                .averageVelocity(Math.round(averageVelocity * 100.0) / 100.0)
                .build();
    }

    // ==================== HABIT ANALYTICS ====================

    public HabitAnalyticsResponse getHabitAnalytics(UUID userId) {
        List<Habit> habits = habitRepository.findByUserId(userId);
        LocalDate today = LocalDate.now();
        LocalDate startDate30 = today.minusDays(30);
        LocalDate startDate90 = today.minusDays(90);

        List<HabitStats> habitStats = new ArrayList<>();
        int overallLongestStreak = 0;
        int overallCurrentStreak = 0;
        String bestStreakHabitName = null;

        Map<String, Integer> completionsByDayOfWeek = new HashMap<>();
        Map<String, Integer> totalByDayOfWeek = new HashMap<>();

        for (Habit habit : habits) {
            List<HabitLog> logs = habitLogRepository.findByHabitId(habit.getId());

            // Filter completed logs
            List<HabitLog> completedLogs = logs.stream()
                    .filter(l -> Boolean.TRUE.equals(l.getCompleted()))
                    .sorted(Comparator.comparing(HabitLog::getDate).reversed())
                    .toList();

            // Calculate streaks
            int currentStreak = calculateCurrentStreak(completedLogs, today);
            int longestStreak = calculateLongestStreak(completedLogs);

            if (longestStreak > overallLongestStreak) {
                overallLongestStreak = longestStreak;
                bestStreakHabitName = habit.getName();
            }

            // Overall current streak contribution
            if (currentStreak > overallCurrentStreak) {
                overallCurrentStreak = currentStreak;
            }

            // Completion rate (last 30 days)
            long completionsLast30Days = completedLogs.stream()
                    .filter(l -> !l.getDate().isBefore(startDate30))
                    .count();

            double completionRate = 30 > 0 ? (double) completionsLast30Days / 30 * 100 : 0;

            String lastCompletedDate = completedLogs.isEmpty() ? null
                    : completedLogs.get(0).getDate().format(DATE_FORMATTER);

            habitStats.add(HabitStats.builder()
                    .habitId(habit.getId().toString())
                    .habitName(habit.getName())
                    .habitIcon(habit.getIcon())
                    .frequency(habit.getFrequency() != null ? habit.getFrequency().name() : null)
                    .currentStreak(currentStreak)
                    .longestStreak(longestStreak)
                    .completionRate(Math.round(completionRate * 100.0) / 100.0)
                    .totalCompletions(completedLogs.size())
                    .lastCompletedDate(lastCompletedDate)
                    .build());

            // Track completions by day of week
            for (HabitLog log : completedLogs) {
                if (!log.getDate().isBefore(startDate30)) {
                    String dayName = log.getDate().getDayOfWeek().name();
                    completionsByDayOfWeek.merge(dayName, 1, Integer::sum);
                }
            }
        }

        // Calculate day of week totals (how many times each day occurred in last 30 days)
        for (LocalDate d = startDate30; !d.isAfter(today); d = d.plusDays(1)) {
            String dayName = d.getDayOfWeek().name();
            totalByDayOfWeek.merge(dayName, habits.size(), Integer::sum);
        }

        // Completion rate by day of week
        Map<String, Double> completionByDayOfWeek = new HashMap<>();
        for (String day : totalByDayOfWeek.keySet()) {
            int total = totalByDayOfWeek.getOrDefault(day, 0);
            int completed = completionsByDayOfWeek.getOrDefault(day, 0);
            completionByDayOfWeek.put(day, total > 0 ? Math.round((double) completed / total * 100 * 100.0) / 100.0 : 0.0);
        }

        // Overall completion rates
        double dailyCompletionRate = calculateOverallCompletionRate(habits, habitLogRepository, today, 1);
        double weeklyCompletionRate = calculateOverallCompletionRate(habits, habitLogRepository, today, 7);
        double monthlyCompletionRate = calculateOverallCompletionRate(habits, habitLogRepository, today, 30);

        // Completion heatmap (last 90 days)
        List<HeatmapData> completionHeatmap = calculateHeatmap(habits, habitLogRepository, startDate90, today);

        // Trends
        List<TrendData> last30DaysTrend = calculateTrend(habits, habitLogRepository, startDate30, today);
        List<TrendData> last90DaysTrend = calculateTrend(habits, habitLogRepository, startDate90, today);

        return HabitAnalyticsResponse.builder()
                .totalHabits(habits.size())
                .dailyCompletionRate(Math.round(dailyCompletionRate * 100.0) / 100.0)
                .weeklyCompletionRate(Math.round(weeklyCompletionRate * 100.0) / 100.0)
                .monthlyCompletionRate(Math.round(monthlyCompletionRate * 100.0) / 100.0)
                .longestStreak(overallLongestStreak)
                .currentStreak(overallCurrentStreak)
                .bestStreakHabitName(bestStreakHabitName)
                .habitStats(habitStats)
                .completionHeatmap(completionHeatmap)
                .completionByDayOfWeek(completionByDayOfWeek)
                .last30DaysTrend(last30DaysTrend)
                .last90DaysTrend(last90DaysTrend)
                .build();
    }

    private int calculateCurrentStreak(List<HabitLog> completedLogs, LocalDate today) {
        if (completedLogs.isEmpty()) return 0;

        Set<LocalDate> completedDates = completedLogs.stream()
                .map(HabitLog::getDate)
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate checkDate = today;

        // Check if completed today or yesterday
        if (!completedDates.contains(today)) {
            checkDate = today.minusDays(1);
        }

        while (completedDates.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }

    private int calculateLongestStreak(List<HabitLog> completedLogs) {
        if (completedLogs.isEmpty()) return 0;

        Set<LocalDate> completedDates = completedLogs.stream()
                .map(HabitLog::getDate)
                .collect(Collectors.toSet());

        List<LocalDate> sortedDates = new ArrayList<>(completedDates);
        Collections.sort(sortedDates);

        int longestStreak = 1;
        int currentStreak = 1;

        for (int i = 1; i < sortedDates.size(); i++) {
            if (sortedDates.get(i).equals(sortedDates.get(i - 1).plusDays(1))) {
                currentStreak++;
                longestStreak = Math.max(longestStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }

        return longestStreak;
    }

    private double calculateOverallCompletionRate(List<Habit> habits, HabitLogRepositoryPort logRepo,
                                                   LocalDate today, int days) {
        if (habits.isEmpty()) return 0;

        LocalDate startDate = today.minusDays(days - 1);
        long totalPossible = (long) habits.size() * days;
        long totalCompleted = 0;

        for (Habit habit : habits) {
            List<HabitLog> logs = logRepo.findByHabitIdAndDateRange(habit.getId(), startDate, today);
            totalCompleted += logs.stream()
                    .filter(l -> Boolean.TRUE.equals(l.getCompleted()))
                    .count();
        }

        return totalPossible > 0 ? (double) totalCompleted / totalPossible * 100 : 0;
    }

    private List<HeatmapData> calculateHeatmap(List<Habit> habits, HabitLogRepositoryPort logRepo,
                                                LocalDate startDate, LocalDate endDate) {
        List<HeatmapData> result = new ArrayList<>();
        int totalHabits = habits.size();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date;
            int completedCount = 0;

            for (Habit habit : habits) {
                Optional<HabitLog> log = logRepo.findByHabitIdAndDate(habit.getId(), currentDate);
                if (log.isPresent() && Boolean.TRUE.equals(log.get().getCompleted())) {
                    completedCount++;
                }
            }

            double rate = totalHabits > 0 ? (double) completedCount / totalHabits * 100 : 0;
            result.add(HeatmapData.builder()
                    .date(currentDate.format(DATE_FORMATTER))
                    .completedCount(completedCount)
                    .totalHabits(totalHabits)
                    .completionRate(Math.round(rate * 100.0) / 100.0)
                    .build());
        }

        return result;
    }

    private List<TrendData> calculateTrend(List<Habit> habits, HabitLogRepositoryPort logRepo,
                                           LocalDate startDate, LocalDate endDate) {
        List<TrendData> result = new ArrayList<>();
        int totalHabits = habits.size();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date;
            int completedCount = 0;

            for (Habit habit : habits) {
                Optional<HabitLog> log = logRepo.findByHabitIdAndDate(habit.getId(), currentDate);
                if (log.isPresent() && Boolean.TRUE.equals(log.get().getCompleted())) {
                    completedCount++;
                }
            }

            double rate = totalHabits > 0 ? (double) completedCount / totalHabits * 100 : 0;
            result.add(TrendData.builder()
                    .date(currentDate.format(DATE_FORMATTER))
                    .completionRate(Math.round(rate * 100.0) / 100.0)
                    .completed(completedCount)
                    .total(totalHabits)
                    .build());
        }

        return result;
    }

    // ==================== ORGANIZATION ANALYTICS ====================

    public OrganizationAnalyticsResponse getOrganizationAnalytics(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        List<OrganizationMember> members = memberRepository.findByOrganizationId(organizationId);
        List<Task> tasks = taskRepository.findByOrganizationId(organizationId);
        List<Goal> goals = goalRepository.findByOrganizationId(organizationId);

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        LocalDate twoWeeksAgo = today.minusDays(14);

        // Overview counts
        long totalMembers = members.size();
        long totalTasks = tasks.size();
        long activeTasks = tasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .count();
        long totalGoals = goals.size();

        // Tasks this week
        long tasksCreatedThisWeek = tasks.stream()
                .filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().toLocalDate().isBefore(weekAgo))
                .count();

        long tasksCompletedThisWeek = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE && t.getUpdatedAt() != null
                        && !t.getUpdatedAt().toLocalDate().isBefore(weekAgo))
                .count();

        // Tasks last week (for trend calculation)
        long tasksCompletedLastWeek = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE && t.getUpdatedAt() != null)
                .filter(t -> {
                    LocalDate d = t.getUpdatedAt().toLocalDate();
                    return !d.isBefore(twoWeeksAgo) && d.isBefore(weekAgo);
                })
                .count();

        // Trend calculation
        double taskCompletionTrend;
        if (tasksCompletedLastWeek > 0) {
            taskCompletionTrend = ((double) tasksCompletedThisWeek - tasksCompletedLastWeek) / tasksCompletedLastWeek * 100;
        } else {
            taskCompletionTrend = tasksCompletedThisWeek > 0 ? 100 : 0;
        }

        // Health score calculation (0-100)
        int healthScore = calculateHealthScore(tasks, members, goals);

        // Monthly growth data (last 6 months)
        List<MonthlyGrowth> monthlyGrowth = calculateMonthlyGrowth(tasks, members, goals, 6);

        return OrganizationAnalyticsResponse.builder()
                .healthScore(healthScore)
                .totalMembers(totalMembers)
                .activeMembers(totalMembers) // Simplified - would need activity tracking
                .totalTasks(totalTasks)
                .activeTasks(activeTasks)
                .totalGoals(totalGoals)
                .totalEvents(0L) // Would need to query events
                .totalNotes(0L) // Would need to query notes
                .totalTeams(0L) // Would need to query teams
                .tasksCreatedThisWeek(tasksCreatedThisWeek)
                .tasksCompletedThisWeek(tasksCompletedThisWeek)
                .taskCompletionTrend(Math.round(taskCompletionTrend * 100.0) / 100.0)
                .memberActivityTrend(0.0) // Simplified
                .monthlyGrowth(monthlyGrowth)
                .build();
    }

    private int calculateHealthScore(List<Task> tasks, List<OrganizationMember> members, List<Goal> goals) {
        // Health score based on multiple factors
        int score = 50; // Base score

        // Task completion rate impact (+/- 20 points)
        long completedTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        double completionRate = tasks.isEmpty() ? 0 : (double) completedTasks / tasks.size();
        score += (int) (completionRate * 20);

        // Overdue tasks impact (-10 points max)
        LocalDate today = LocalDate.now();
        long overdueTasks = tasks.stream()
                .filter(t -> t.getDueDate() != null
                        && t.getDueDate().toLocalDate().isBefore(today)
                        && t.getStatus() != TaskStatus.DONE)
                .count();
        double overdueRate = tasks.isEmpty() ? 0 : (double) overdueTasks / tasks.size();
        score -= (int) (overdueRate * 10);

        // Team size impact (+10 points for 5+ members)
        if (members.size() >= 5) {
            score += 10;
        }

        // Goal presence impact (+10 points)
        if (!goals.isEmpty()) {
            score += 10;
        }

        // Active work impact (+10 points if there are in-progress tasks)
        long inProgressTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        if (inProgressTasks > 0) {
            score += 10;
        }

        return Math.max(0, Math.min(100, score));
    }

    private List<MonthlyGrowth> calculateMonthlyGrowth(List<Task> tasks, List<OrganizationMember> members,
                                                        List<Goal> goals, int months) {
        List<MonthlyGrowth> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            String monthLabel = monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM"));

            long tasksCreated = tasks.stream()
                    .filter(t -> t.getCreatedAt() != null)
                    .filter(t -> {
                        LocalDate d = t.getCreatedAt().toLocalDate();
                        return !d.isBefore(monthStart) && !d.isAfter(monthEnd);
                    })
                    .count();

            long tasksCompleted = tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE && t.getUpdatedAt() != null)
                    .filter(t -> {
                        LocalDate d = t.getUpdatedAt().toLocalDate();
                        return !d.isBefore(monthStart) && !d.isAfter(monthEnd);
                    })
                    .count();

            long newMembers = members.stream()
                    .filter(m -> m.getJoinedAt() != null)
                    .filter(m -> {
                        LocalDate d = m.getJoinedAt().toLocalDate();
                        return !d.isBefore(monthStart) && !d.isAfter(monthEnd);
                    })
                    .count();

            result.add(MonthlyGrowth.builder()
                    .month(monthLabel)
                    .tasksCreated(tasksCreated)
                    .tasksCompleted(tasksCompleted)
                    .newMembers(newMembers)
                    .goalsCompleted(0L) // Would need goal completion tracking
                    .build());
        }

        return result;
    }
}
