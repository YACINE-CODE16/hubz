package com.hubz.application.service;

import com.hubz.application.dto.request.AnalyticsFilterRequest;
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
import org.springframework.cache.annotation.Cacheable;
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
    private final TeamRepositoryPort teamRepository;
    private final TeamMemberRepositoryPort teamMemberRepository;
    private final EventRepositoryPort eventRepository;
    private final NoteRepositoryPort noteRepository;
    private final TaskCommentRepositoryPort taskCommentRepository;

    private static final int OVERLOAD_THRESHOLD = 10; // tasks considered as overloaded
    private static final int DEFAULT_INACTIVE_DAYS_THRESHOLD = 14;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==================== FILTER LOGIC ====================

    /**
     * Apply dynamic filters to a list of tasks.
     * Filters by date range (on createdAt), member/assignee IDs, statuses, and priorities.
     */
    List<Task> applyFilters(List<Task> tasks, AnalyticsFilterRequest filters) {
        if (filters == null || !filters.hasAnyFilter()) {
            return tasks;
        }

        return tasks.stream()
                .filter(t -> {
                    // Date range filter (based on createdAt)
                    if (filters.getStartDate() != null && t.getCreatedAt() != null) {
                        if (t.getCreatedAt().toLocalDate().isBefore(filters.getStartDate())) {
                            return false;
                        }
                    }
                    if (filters.getEndDate() != null && t.getCreatedAt() != null) {
                        if (t.getCreatedAt().toLocalDate().isAfter(filters.getEndDate())) {
                            return false;
                        }
                    }
                    // Member / assignee filter
                    if (filters.getMemberIds() != null && !filters.getMemberIds().isEmpty()) {
                        if (t.getAssigneeId() == null || !filters.getMemberIds().contains(t.getAssigneeId())) {
                            return false;
                        }
                    }
                    // Status filter
                    if (filters.getStatuses() != null && !filters.getStatuses().isEmpty()) {
                        if (t.getStatus() == null || !filters.getStatuses().contains(t.getStatus())) {
                            return false;
                        }
                    }
                    // Priority filter
                    if (filters.getPriorities() != null && !filters.getPriorities().isEmpty()) {
                        if (t.getPriority() == null || !filters.getPriorities().contains(t.getPriority())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    // ==================== TASK ANALYTICS ====================

    /**
     * Get task analytics without filters (backward-compatible).
     * Result is cached in the "analytics" cache with orgId as key.
     */
    @Cacheable(value = "analytics", key = "#organizationId")
    public TaskAnalyticsResponse getTaskAnalytics(UUID organizationId, UUID currentUserId) {
        return getTaskAnalytics(organizationId, currentUserId, null);
    }

    /**
     * Get task analytics with optional dynamic filters.
     */
    public TaskAnalyticsResponse getTaskAnalytics(UUID organizationId, UUID currentUserId, AnalyticsFilterRequest filters) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        List<Task> allTasks = taskRepository.findByOrganizationId(organizationId);
        List<Task> tasks = applyFilters(allTasks, filters);
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

        // Advanced analytics - Burnup chart
        List<BurnupData> burnupChart = calculateBurnupChart(tasks, 30);

        // Advanced analytics - Throughput chart with 7-day rolling average
        List<ThroughputData> throughputChart = calculateThroughputChart(tasks, 30);

        // Advanced analytics - Cycle time distribution
        List<CycleTimeBucket> cycleTimeDistribution = calculateCycleTimeDistribution(tasks);

        // Advanced analytics - Lead time trend (last 12 weeks)
        List<LeadTimeData> leadTimeTrend = calculateLeadTimeTrend(tasks, 12);
        Double averageLeadTimeHours = calculateAverageLeadTime(tasks);

        // Advanced analytics - WIP chart
        List<WIPData> wipChart = calculateWIPChart(tasks, 30);
        Double averageWIP = calculateAverageWIP(wipChart);

        // Status time tracking
        Double avgTimeInTodoHours = calculateAverageTimeInStatus(tasks, TaskStatus.TODO);
        Double avgTimeInProgressHours = calculateAverageTimeInStatus(tasks, TaskStatus.IN_PROGRESS);

        return TaskAnalyticsResponse.builder()
                .totalTasks(totalTasks)
                .todoCount(todoCount)
                .inProgressCount(inProgressCount)
                .doneCount(doneCount)
                .completionRate(Math.round(completionRate * 100.0) / 100.0)
                .overdueCount(overdueCount)
                .overdueRate(Math.round(overdueRate * 100.0) / 100.0)
                .averageCompletionTimeHours(avgCompletionTimeHours)
                .averageTimeInTodoHours(avgTimeInTodoHours)
                .averageTimeInProgressHours(avgTimeInProgressHours)
                .tasksByPriority(tasksByPriority)
                .tasksByStatus(tasksByStatus)
                .tasksCreatedOverTime(tasksCreatedOverTime)
                .tasksCompletedOverTime(tasksCompletedOverTime)
                .burndownChart(burndownChart)
                .burnupChart(burnupChart)
                .velocityChart(velocityChart)
                .cumulativeFlowDiagram(cumulativeFlowDiagram)
                .throughputChart(throughputChart)
                .cycleTimeDistribution(cycleTimeDistribution)
                .leadTimeTrend(leadTimeTrend)
                .averageLeadTimeHours(averageLeadTimeHours)
                .wipChart(wipChart)
                .averageWIP(averageWIP)
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

    // ==================== ADVANCED TASK ANALYTICS ====================

    private List<BurnupData> calculateBurnupChart(List<Task> tasks, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        List<BurnupData> result = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date;

            // Cumulative completed: all tasks completed up to this date
            long cumulativeCompleted = tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE
                            && t.getUpdatedAt() != null
                            && !t.getUpdatedAt().toLocalDate().isAfter(currentDate))
                    .count();

            // Total scope: all tasks created up to this date
            long totalScope = tasks.stream()
                    .filter(t -> t.getCreatedAt() != null
                            && !t.getCreatedAt().toLocalDate().isAfter(currentDate))
                    .count();

            result.add(BurnupData.builder()
                    .date(currentDate.format(DATE_FORMATTER))
                    .cumulativeCompleted(cumulativeCompleted)
                    .totalScope(totalScope)
                    .build());
        }
        return result;
    }

    private List<ThroughputData> calculateThroughputChart(List<Task> tasks, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        // First, calculate daily completions
        Map<LocalDate, Long> dailyCompletions = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE && t.getUpdatedAt() != null)
                .filter(t -> !t.getUpdatedAt().toLocalDate().isBefore(startDate))
                .collect(Collectors.groupingBy(
                        t -> t.getUpdatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        List<ThroughputData> result = new ArrayList<>();
        List<Long> last7Days = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            long completedCount = dailyCompletions.getOrDefault(date, 0L);

            // Maintain rolling window
            last7Days.add(completedCount);
            if (last7Days.size() > 7) {
                last7Days.remove(0);
            }

            // Calculate 7-day rolling average
            double rollingAverage = last7Days.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);

            result.add(ThroughputData.builder()
                    .date(date.format(DATE_FORMATTER))
                    .completedCount(completedCount)
                    .rollingAverage(Math.round(rollingAverage * 100.0) / 100.0)
                    .build());
        }
        return result;
    }

    private List<CycleTimeBucket> calculateCycleTimeDistribution(List<Task> tasks) {
        // Define buckets: <1 day, 1-3 days, 3-7 days, 1-2 weeks, 2-4 weeks, >1 month
        List<CycleTimeBucket> buckets = new ArrayList<>();

        // Get completed tasks with valid timestamps
        List<Task> completedTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE
                        && t.getCreatedAt() != null
                        && t.getUpdatedAt() != null)
                .toList();

        if (completedTasks.isEmpty()) {
            // Return empty buckets with zero counts
            buckets.add(createBucket("<1 jour", 0, 24, 0, 0));
            buckets.add(createBucket("1-3 jours", 24, 72, 0, 0));
            buckets.add(createBucket("3-7 jours", 72, 168, 0, 0));
            buckets.add(createBucket("1-2 semaines", 168, 336, 0, 0));
            buckets.add(createBucket("2-4 semaines", 336, 672, 0, 0));
            buckets.add(createBucket(">1 mois", 672, -1, 0, 0));
            return buckets;
        }

        // Calculate cycle time for each task (in hours)
        Map<String, Long> bucketCounts = new LinkedHashMap<>();
        bucketCounts.put("<1 jour", 0L);
        bucketCounts.put("1-3 jours", 0L);
        bucketCounts.put("3-7 jours", 0L);
        bucketCounts.put("1-2 semaines", 0L);
        bucketCounts.put("2-4 semaines", 0L);
        bucketCounts.put(">1 mois", 0L);

        for (Task task : completedTasks) {
            long hours = ChronoUnit.HOURS.between(task.getCreatedAt(), task.getUpdatedAt());

            if (hours < 24) {
                bucketCounts.merge("<1 jour", 1L, Long::sum);
            } else if (hours < 72) {
                bucketCounts.merge("1-3 jours", 1L, Long::sum);
            } else if (hours < 168) {
                bucketCounts.merge("3-7 jours", 1L, Long::sum);
            } else if (hours < 336) {
                bucketCounts.merge("1-2 semaines", 1L, Long::sum);
            } else if (hours < 672) {
                bucketCounts.merge("2-4 semaines", 1L, Long::sum);
            } else {
                bucketCounts.merge(">1 mois", 1L, Long::sum);
            }
        }

        long total = completedTasks.size();
        buckets.add(createBucket("<1 jour", 0, 24, bucketCounts.get("<1 jour"), total));
        buckets.add(createBucket("1-3 jours", 24, 72, bucketCounts.get("1-3 jours"), total));
        buckets.add(createBucket("3-7 jours", 72, 168, bucketCounts.get("3-7 jours"), total));
        buckets.add(createBucket("1-2 semaines", 168, 336, bucketCounts.get("1-2 semaines"), total));
        buckets.add(createBucket("2-4 semaines", 336, 672, bucketCounts.get("2-4 semaines"), total));
        buckets.add(createBucket(">1 mois", 672, -1, bucketCounts.get(">1 mois"), total));

        return buckets;
    }

    private CycleTimeBucket createBucket(String name, int minHours, int maxHours, long count, long total) {
        double percentage = total > 0 ? Math.round((double) count / total * 100 * 100.0) / 100.0 : 0;
        return CycleTimeBucket.builder()
                .bucket(name)
                .minHours(minHours)
                .maxHours(maxHours)
                .count(count)
                .percentage(percentage)
                .build();
    }

    private List<LeadTimeData> calculateLeadTimeTrend(List<Task> tasks, int weeks) {
        LocalDate endDate = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        List<LeadTimeData> result = new ArrayList<>();

        for (int i = weeks - 1; i >= 0; i--) {
            LocalDate weekStart = endDate.minusWeeks(i).with(weekFields.dayOfWeek(), 1);
            LocalDate weekEnd = weekStart.plusDays(6);

            // Get tasks completed in this week
            List<Task> weekTasks = tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE
                            && t.getCreatedAt() != null
                            && t.getUpdatedAt() != null)
                    .filter(t -> {
                        LocalDate completedDate = t.getUpdatedAt().toLocalDate();
                        return !completedDate.isBefore(weekStart) && !completedDate.isAfter(weekEnd);
                    })
                    .toList();

            Double averageLeadTime = null;
            if (!weekTasks.isEmpty()) {
                double totalHours = weekTasks.stream()
                        .mapToDouble(t -> ChronoUnit.HOURS.between(t.getCreatedAt(), t.getUpdatedAt()))
                        .sum();
                averageLeadTime = Math.round(totalHours / weekTasks.size() * 100.0) / 100.0;
            }

            result.add(LeadTimeData.builder()
                    .date(weekStart.format(DATE_FORMATTER))
                    .averageLeadTimeHours(averageLeadTime)
                    .taskCount(weekTasks.size())
                    .build());
        }
        return result;
    }

    private Double calculateAverageLeadTime(List<Task> tasks) {
        List<Task> completedTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE
                        && t.getCreatedAt() != null
                        && t.getUpdatedAt() != null)
                .toList();

        if (completedTasks.isEmpty()) {
            return null;
        }

        double totalHours = completedTasks.stream()
                .mapToDouble(t -> ChronoUnit.HOURS.between(t.getCreatedAt(), t.getUpdatedAt()))
                .sum();

        return Math.round(totalHours / completedTasks.size() * 100.0) / 100.0;
    }

    private List<WIPData> calculateWIPChart(List<Task> tasks, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        List<WIPData> result = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date;

            // Count tasks in IN_PROGRESS status on this date
            // A task is in progress if it was created before/on this date and not completed before this date
            long wipCount = tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS
                            && t.getCreatedAt() != null
                            && !t.getCreatedAt().toLocalDate().isAfter(currentDate))
                    .count();

            // Count tasks in TODO status on this date
            long todoCount = tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.TODO
                            && t.getCreatedAt() != null
                            && !t.getCreatedAt().toLocalDate().isAfter(currentDate))
                    .count();

            result.add(WIPData.builder()
                    .date(currentDate.format(DATE_FORMATTER))
                    .wipCount(wipCount)
                    .todoCount(todoCount)
                    .totalActive(wipCount + todoCount)
                    .build());
        }
        return result;
    }

    private Double calculateAverageWIP(List<WIPData> wipData) {
        if (wipData.isEmpty()) {
            return 0.0;
        }
        double average = wipData.stream()
                .mapToLong(WIPData::getWipCount)
                .average()
                .orElse(0.0);
        return Math.round(average * 100.0) / 100.0;
    }

    private Double calculateAverageTimeInStatus(List<Task> tasks, TaskStatus status) {
        // This is a simplified implementation
        // In a production system, you'd need task history to track time in each status
        // For now, we estimate based on current state

        if (status == TaskStatus.IN_PROGRESS) {
            // For in-progress tasks, calculate time since creation
            List<Task> inProgressTasks = tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS && t.getCreatedAt() != null)
                    .toList();

            if (inProgressTasks.isEmpty()) {
                return null;
            }

            LocalDateTime now = LocalDateTime.now();
            double totalHours = inProgressTasks.stream()
                    .mapToDouble(t -> ChronoUnit.HOURS.between(t.getCreatedAt(), now))
                    .sum();

            return Math.round(totalHours / inProgressTasks.size() * 100.0) / 100.0;
        }

        // For TODO status, we use a similar approach
        List<Task> todoTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO && t.getCreatedAt() != null)
                .toList();

        if (todoTasks.isEmpty()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        double totalHours = todoTasks.stream()
                .mapToDouble(t -> ChronoUnit.HOURS.between(t.getCreatedAt(), now))
                .sum();

        return Math.round(totalHours / todoTasks.size() * 100.0) / 100.0;
    }

    // ==================== MEMBER ANALYTICS ====================

    /**
     * Get member analytics without filters (backward-compatible).
     */
    public MemberAnalyticsResponse getMemberAnalytics(UUID organizationId, UUID currentUserId) {
        return getMemberAnalytics(organizationId, currentUserId, null);
    }

    /**
     * Get member analytics with optional dynamic filters.
     */
    public MemberAnalyticsResponse getMemberAnalytics(UUID organizationId, UUID currentUserId, AnalyticsFilterRequest filters) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        List<OrganizationMember> members = memberRepository.findByOrganizationId(organizationId);
        List<Task> allTasks = taskRepository.findByOrganizationId(organizationId);
        List<Task> tasks = applyFilters(allTasks, filters);
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

        // Average completion time per member (sorted fastest to slowest)
        List<MemberCompletionTime> memberCompletionTimes = calculateMemberCompletionTimes(tasks, members);

        // Inactive members detection
        List<InactiveMember> inactiveMembers = detectInactiveMembers(organizationId, members, tasks, DEFAULT_INACTIVE_DAYS_THRESHOLD);

        // Team performance comparison
        List<TeamPerformanceComparison> teamPerformanceComparison = compareTeamPerformance(organizationId, tasks);

        // Member workload heatmap
        List<MemberWorkloadHeatmapEntry> memberWorkloadHeatmap = calculateMemberWorkloadHeatmap(tasks, members);

        return MemberAnalyticsResponse.builder()
                .memberProductivity(memberProductivity)
                .memberWorkload(memberWorkload)
                .topPerformers(topPerformers)
                .overloadedMembers(overloadedMembers)
                .activityHeatmap(activityHeatmap)
                .memberCompletionTimes(memberCompletionTimes)
                .inactiveMembers(inactiveMembers)
                .teamPerformanceComparison(teamPerformanceComparison)
                .memberWorkloadHeatmap(memberWorkloadHeatmap)
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

    // ==================== MEMBER COMPLETION TIME ====================

    /**
     * Calculate average completion time per member, sorted from fastest to slowest.
     */
    private List<MemberCompletionTime> calculateMemberCompletionTimes(List<Task> tasks, List<OrganizationMember> members) {
        List<MemberCompletionTime> result = new ArrayList<>();

        for (OrganizationMember member : members) {
            User user = userRepository.findById(member.getUserId()).orElse(null);
            if (user == null) continue;

            String memberName = user.getFirstName() + " " + user.getLastName();
            String memberEmail = user.getEmail();

            // Get completed tasks for this member with valid timestamps
            List<Task> completedTasks = tasks.stream()
                    .filter(t -> member.getUserId().equals(t.getAssigneeId()))
                    .filter(t -> t.getStatus() == TaskStatus.DONE
                            && t.getCreatedAt() != null
                            && t.getUpdatedAt() != null)
                    .toList();

            Double avgHours = null;
            if (!completedTasks.isEmpty()) {
                double totalHours = completedTasks.stream()
                        .mapToDouble(t -> ChronoUnit.HOURS.between(t.getCreatedAt(), t.getUpdatedAt()))
                        .sum();
                avgHours = Math.round(totalHours / completedTasks.size() * 100.0) / 100.0;
            }

            result.add(MemberCompletionTime.builder()
                    .memberId(member.getUserId().toString())
                    .memberName(memberName)
                    .memberEmail(memberEmail)
                    .averageCompletionTimeHours(avgHours)
                    .tasksCompleted(completedTasks.size())
                    .build());
        }

        // Sort by average completion time ascending (fastest first), nulls last
        result.sort((a, b) -> {
            if (a.getAverageCompletionTimeHours() == null && b.getAverageCompletionTimeHours() == null) return 0;
            if (a.getAverageCompletionTimeHours() == null) return 1;
            if (b.getAverageCompletionTimeHours() == null) return -1;
            return Double.compare(a.getAverageCompletionTimeHours(), b.getAverageCompletionTimeHours());
        });

        // Assign ranks
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }

        return result;
    }

    // ==================== INACTIVE MEMBERS ====================

    /**
     * Detect inactive members: members with no task completions, no task creations,
     * no comments, no events, and no notes created within the specified number of days.
     */
    private List<InactiveMember> detectInactiveMembers(UUID organizationId, List<OrganizationMember> members,
                                                        List<Task> tasks, int daysThreshold) {
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.minusDays(daysThreshold);
        LocalDateTime thresholdDateTime = thresholdDate.atStartOfDay();

        // Load events and notes for the organization
        List<Event> events = eventRepository.findByOrganizationId(organizationId);
        List<Note> notes = noteRepository.findByOrganizationId(organizationId);

        List<InactiveMember> result = new ArrayList<>();

        for (OrganizationMember member : members) {
            User user = userRepository.findById(member.getUserId()).orElse(null);
            if (user == null) continue;

            UUID memberId = member.getUserId();
            String memberName = user.getFirstName() + " " + user.getLastName();
            String memberEmail = user.getEmail();

            // Find the last activity date across all activity types
            LocalDateTime lastActivity = null;

            // 1. Check task completions (assigned tasks that were completed)
            Optional<LocalDateTime> lastTaskCompletion = tasks.stream()
                    .filter(t -> memberId.equals(t.getAssigneeId())
                            && t.getStatus() == TaskStatus.DONE
                            && t.getUpdatedAt() != null)
                    .map(Task::getUpdatedAt)
                    .max(Comparator.naturalOrder());
            if (lastTaskCompletion.isPresent()) {
                lastActivity = lastTaskCompletion.get();
            }

            // 2. Check task creation
            Optional<LocalDateTime> lastTaskCreation = tasks.stream()
                    .filter(t -> memberId.equals(t.getCreatorId()) && t.getCreatedAt() != null)
                    .map(Task::getCreatedAt)
                    .max(Comparator.naturalOrder());
            if (lastTaskCreation.isPresent() && (lastActivity == null || lastTaskCreation.get().isAfter(lastActivity))) {
                lastActivity = lastTaskCreation.get();
            }

            // 3. Check events created by this member
            Optional<LocalDateTime> lastEventCreation = events.stream()
                    .filter(e -> memberId.equals(e.getUserId()) && e.getCreatedAt() != null)
                    .map(Event::getCreatedAt)
                    .max(Comparator.naturalOrder());
            if (lastEventCreation.isPresent() && (lastActivity == null || lastEventCreation.get().isAfter(lastActivity))) {
                lastActivity = lastEventCreation.get();
            }

            // 4. Check notes created by this member
            Optional<LocalDateTime> lastNoteCreation = notes.stream()
                    .filter(n -> memberId.equals(n.getCreatedById()) && n.getCreatedAt() != null)
                    .map(Note::getCreatedAt)
                    .max(Comparator.naturalOrder());
            if (lastNoteCreation.isPresent() && (lastActivity == null || lastNoteCreation.get().isAfter(lastActivity))) {
                lastActivity = lastNoteCreation.get();
            }

            // Determine if inactive
            boolean isInactive = lastActivity == null || lastActivity.isBefore(thresholdDateTime);

            if (isInactive) {
                long inactiveDays = lastActivity != null
                        ? ChronoUnit.DAYS.between(lastActivity.toLocalDate(), today)
                        : ChronoUnit.DAYS.between(member.getJoinedAt() != null ? member.getJoinedAt().toLocalDate() : today, today);

                result.add(InactiveMember.builder()
                        .memberId(memberId.toString())
                        .memberName(memberName)
                        .memberEmail(memberEmail)
                        .lastActivityDate(lastActivity != null ? lastActivity.toLocalDate().format(DATE_FORMATTER) : null)
                        .inactiveDays(inactiveDays)
                        .build());
            }
        }

        // Sort by most inactive first
        result.sort((a, b) -> Long.compare(b.getInactiveDays(), a.getInactiveDays()));

        return result;
    }

    // ==================== TEAM PERFORMANCE COMPARISON ====================

    /**
     * Compare performance across teams in an organization.
     * Metrics: avg tasks completed, avg completion time, team velocity (tasks per week over last 4 weeks).
     */
    private List<TeamPerformanceComparison> compareTeamPerformance(UUID organizationId, List<Task> tasks) {
        List<Team> teams = teamRepository.findByOrganizationId(organizationId);
        List<TeamPerformanceComparison> result = new ArrayList<>();

        LocalDate today = LocalDate.now();
        LocalDate fourWeeksAgo = today.minusWeeks(4);
        LocalDateTime fourWeeksAgoDateTime = fourWeeksAgo.atStartOfDay();

        for (Team team : teams) {
            List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(team.getId());
            Set<UUID> teamMemberIds = teamMembers.stream()
                    .map(TeamMember::getUserId)
                    .collect(Collectors.toSet());

            if (teamMemberIds.isEmpty()) {
                result.add(TeamPerformanceComparison.builder()
                        .teamId(team.getId().toString())
                        .teamName(team.getName())
                        .memberCount(0)
                        .totalTasksCompleted(0)
                        .avgTasksCompletedPerMember(0)
                        .avgCompletionTimeHours(null)
                        .teamVelocity(0)
                        .completionRate(0)
                        .build());
                continue;
            }

            // Tasks assigned to team members
            List<Task> teamTasks = tasks.stream()
                    .filter(t -> t.getAssigneeId() != null && teamMemberIds.contains(t.getAssigneeId()))
                    .toList();

            long totalCompleted = teamTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE)
                    .count();

            long totalTeamTasks = teamTasks.size();
            double completionRate = totalTeamTasks > 0 ? (double) totalCompleted / totalTeamTasks * 100 : 0;

            double avgTasksPerMember = teamMemberIds.isEmpty() ? 0 : (double) totalCompleted / teamMemberIds.size();

            // Average completion time
            List<Task> completedTasks = teamTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE
                            && t.getCreatedAt() != null
                            && t.getUpdatedAt() != null)
                    .toList();

            Double avgCompletionTime = null;
            if (!completedTasks.isEmpty()) {
                double totalHours = completedTasks.stream()
                        .mapToDouble(t -> ChronoUnit.HOURS.between(t.getCreatedAt(), t.getUpdatedAt()))
                        .sum();
                avgCompletionTime = Math.round(totalHours / completedTasks.size() * 100.0) / 100.0;
            }

            // Team velocity: tasks completed in last 4 weeks, divided by 4
            long recentCompleted = teamTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE
                            && t.getUpdatedAt() != null
                            && t.getUpdatedAt().isAfter(fourWeeksAgoDateTime))
                    .count();
            double velocity = Math.round((double) recentCompleted / 4 * 100.0) / 100.0;

            result.add(TeamPerformanceComparison.builder()
                    .teamId(team.getId().toString())
                    .teamName(team.getName())
                    .memberCount(teamMemberIds.size())
                    .totalTasksCompleted(totalCompleted)
                    .avgTasksCompletedPerMember(Math.round(avgTasksPerMember * 100.0) / 100.0)
                    .avgCompletionTimeHours(avgCompletionTime)
                    .teamVelocity(velocity)
                    .completionRate(Math.round(completionRate * 100.0) / 100.0)
                    .build());
        }

        // Sort by total tasks completed descending
        result.sort((a, b) -> Long.compare(b.getTotalTasksCompleted(), a.getTotalTasksCompleted()));

        return result;
    }

    // ==================== MEMBER WORKLOAD HEATMAP ====================

    /**
     * Calculate workload heatmap: for each member, count active tasks per day of week.
     * Uses task due dates to determine which day a task falls on.
     * If no due date, uses createdAt date.
     */
    private List<MemberWorkloadHeatmapEntry> calculateMemberWorkloadHeatmap(List<Task> tasks, List<OrganizationMember> members) {
        List<MemberWorkloadHeatmapEntry> result = new ArrayList<>();

        // Use the last 30 days of completed/active tasks to build the heatmap
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(30);

        for (OrganizationMember member : members) {
            User user = userRepository.findById(member.getUserId()).orElse(null);
            if (user == null) continue;

            String memberName = user.getFirstName() + " " + user.getLastName();
            UUID memberId = member.getUserId();

            // Tasks assigned to this member
            List<Task> memberTasks = tasks.stream()
                    .filter(t -> memberId.equals(t.getAssigneeId()))
                    .toList();

            // Count tasks per day of week using task activity dates
            Map<String, Long> tasksByDay = new LinkedHashMap<>();
            // Initialize all days with 0
            for (DayOfWeek day : DayOfWeek.values()) {
                tasksByDay.put(day.name(), 0L);
            }

            // Count task activity by day of week over the last 30 days
            for (Task task : memberTasks) {
                // Use updatedAt for completed tasks, dueDate if set, otherwise createdAt
                LocalDate taskDate = null;
                if (task.getStatus() == TaskStatus.DONE && task.getUpdatedAt() != null) {
                    taskDate = task.getUpdatedAt().toLocalDate();
                } else if (task.getDueDate() != null) {
                    taskDate = task.getDueDate().toLocalDate();
                } else if (task.getCreatedAt() != null) {
                    taskDate = task.getCreatedAt().toLocalDate();
                }

                if (taskDate != null && !taskDate.isBefore(startDate) && !taskDate.isAfter(today)) {
                    String dayName = taskDate.getDayOfWeek().name();
                    tasksByDay.merge(dayName, 1L, Long::sum);
                }
            }

            result.add(MemberWorkloadHeatmapEntry.builder()
                    .memberId(memberId.toString())
                    .memberName(memberName)
                    .tasksByDayOfWeek(tasksByDay)
                    .build());
        }

        return result;
    }

    // ==================== GOAL ANALYTICS ====================

    /**
     * Get goal analytics without filters (backward-compatible).
     */
    public GoalAnalyticsResponse getGoalAnalytics(UUID organizationId, UUID currentUserId) {
        return getGoalAnalytics(organizationId, currentUserId, null);
    }

    /**
     * Get goal analytics with optional dynamic filters.
     * Filters are applied to the tasks linked to each goal.
     */
    public GoalAnalyticsResponse getGoalAnalytics(UUID organizationId, UUID currentUserId, AnalyticsFilterRequest filters) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        List<Goal> goals = goalRepository.findByOrganizationId(organizationId);
        List<Task> allTasks = taskRepository.findByOrganizationId(organizationId);
        List<Task> tasks = applyFilters(allTasks, filters);
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
