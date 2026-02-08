package com.hubz.application.service;

import com.hubz.application.dto.response.EventResponse;
import com.hubz.application.dto.response.GoalResponse;
import com.hubz.application.dto.response.HabitResponse;
import com.hubz.application.dto.response.PersonalDashboardResponse;
import com.hubz.application.dto.response.PersonalDashboardResponse.DashboardStats;
import com.hubz.application.dto.response.PersonalDashboardResponse.HabitWithStatusResponse;
import com.hubz.application.dto.response.TaskResponse;
import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.HabitLogRepositoryPort;
import com.hubz.application.port.out.HabitRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.model.Event;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.Habit;
import com.hubz.domain.model.HabitLog;
import com.hubz.domain.model.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalDashboardService {

    private final TaskRepositoryPort taskRepository;
    private final HabitRepositoryPort habitRepository;
    private final HabitLogRepositoryPort habitLogRepository;
    private final EventRepositoryPort eventRepository;
    private final GoalRepositoryPort goalRepository;

    public PersonalDashboardResponse getDashboard(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in7Days = now.plusDays(7);

        // Get all user's tasks
        List<Task> allTasks = taskRepository.findByAssigneeId(userId);

        // Filter for today's tasks (due today or overdue and not completed)
        List<Task> todayTasks = allTasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .filter(task -> {
                    if (task.getDueDate() == null) return false;
                    LocalDate dueDate = task.getDueDate().toLocalDate();
                    return dueDate.isEqual(today) || dueDate.isBefore(today);
                })
                .sorted((a, b) -> {
                    // Sort by priority (URGENT first) then by due date
                    if (a.getPriority() != null && b.getPriority() != null) {
                        int priorityCompare = getPriorityOrder(b.getPriority()) - getPriorityOrder(a.getPriority());
                        if (priorityCompare != 0) return priorityCompare;
                    }
                    if (a.getDueDate() != null && b.getDueDate() != null) {
                        return a.getDueDate().compareTo(b.getDueDate());
                    }
                    return 0;
                })
                .limit(10)
                .toList();

        // Get habits and their logs
        List<Habit> habits = habitRepository.findByUserId(userId);
        List<UUID> habitIds = habits.stream().map(Habit::getId).toList();

        LocalDate startDate = today.minusDays(30);
        List<HabitLog> allLogs = habitIds.isEmpty() ?
                List.of() :
                habitLogRepository.findByHabitIdInAndDateRange(habitIds, startDate, today);

        // Create a map of habit logs by habit ID and date
        Map<UUID, Map<LocalDate, HabitLog>> logsByHabit = new HashMap<>();
        for (HabitLog log : allLogs) {
            logsByHabit.computeIfAbsent(log.getHabitId(), k -> new HashMap<>())
                    .put(log.getDate(), log);
        }

        // Build habit with status responses
        List<HabitWithStatusResponse> todayHabits = new ArrayList<>();
        int completedHabitsToday = 0;
        int longestStreak = 0;

        for (Habit habit : habits) {
            Map<LocalDate, HabitLog> logs = logsByHabit.getOrDefault(habit.getId(), new HashMap<>());
            boolean completedToday = logs.containsKey(today) && logs.get(today).getCompleted();
            if (completedToday) completedHabitsToday++;

            int streak = calculateStreak(logs, today);
            if (streak > longestStreak) longestStreak = streak;

            int completedLast7Days = countCompletedInRange(logs, today.minusDays(6), today);

            todayHabits.add(HabitWithStatusResponse.builder()
                    .habit(toHabitResponse(habit))
                    .completedToday(completedToday)
                    .currentStreak(streak)
                    .completedLast7Days(completedLast7Days)
                    .build());
        }

        // Get upcoming events
        List<Event> upcomingEvents = eventRepository.findPersonalEventsByTimeRange(userId, now, in7Days)
                .stream()
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .limit(5)
                .toList();

        // Get personal goals
        List<Goal> goals = goalRepository.findPersonalGoals(userId);

        // Calculate goal stats by counting tasks associated with each goal
        Map<UUID, List<Task>> tasksByGoal = allTasks.stream()
                .filter(t -> t.getGoalId() != null)
                .collect(Collectors.groupingBy(Task::getGoalId));

        List<GoalResponse> goalResponses = goals.stream()
                .map(goal -> toGoalResponse(goal, tasksByGoal.getOrDefault(goal.getId(), List.of())))
                .toList();

        // Calculate stats
        int completedGoals = (int) goalResponses.stream()
                .filter(g -> g.getTotalTasks() > 0 && g.getCompletedTasks().equals(g.getTotalTasks()))
                .count();

        int completedTasks = (int) allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();

        int overdueTasks = (int) allTasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .filter(t -> t.getDueDate() != null && t.getDueDate().toLocalDate().isBefore(today))
                .count();

        DashboardStats stats = DashboardStats.builder()
                .totalGoals(goals.size())
                .completedGoals(completedGoals)
                .totalTasks(allTasks.size())
                .completedTasks(completedTasks)
                .overdueTasks(overdueTasks)
                .todayTasksCount(todayTasks.size())
                .totalHabits(habits.size())
                .completedHabitsToday(completedHabitsToday)
                .currentStreak(longestStreak)
                .upcomingEventsCount(upcomingEvents.size())
                .build();

        return PersonalDashboardResponse.builder()
                .stats(stats)
                .todayTasks(todayTasks.stream().map(this::toTaskResponse).toList())
                .todayHabits(todayHabits)
                .upcomingEvents(upcomingEvents.stream().map(this::toEventResponse).toList())
                .goals(goalResponses)
                .build();
    }

    private int getPriorityOrder(com.hubz.domain.enums.TaskPriority priority) {
        return switch (priority) {
            case URGENT -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private int calculateStreak(Map<LocalDate, HabitLog> logs, LocalDate today) {
        int streak = 0;
        LocalDate date = today;

        while (true) {
            HabitLog log = logs.get(date);
            if (log == null || !log.getCompleted()) {
                break;
            }
            streak++;
            date = date.minusDays(1);
        }

        return streak;
    }

    private int countCompletedInRange(Map<LocalDate, HabitLog> logs, LocalDate startDate, LocalDate endDate) {
        int count = 0;
        LocalDate date = startDate;

        while (!date.isAfter(endDate)) {
            HabitLog log = logs.get(date);
            if (log != null && log.getCompleted()) {
                count++;
            }
            date = date.plusDays(1);
        }

        return count;
    }

    private TaskResponse toTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .organizationId(task.getOrganizationId())
                .goalId(task.getGoalId())
                .assigneeId(task.getAssigneeId())
                .creatorId(task.getCreatorId())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private HabitResponse toHabitResponse(Habit habit) {
        return HabitResponse.builder()
                .id(habit.getId())
                .name(habit.getName())
                .icon(habit.getIcon())
                .frequency(habit.getFrequency())
                .userId(habit.getUserId())
                .createdAt(habit.getCreatedAt())
                .updatedAt(habit.getUpdatedAt())
                .build();
    }

    private EventResponse toEventResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .objective(event.getObjective())
                .organizationId(event.getOrganizationId())
                .userId(event.getUserId())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    private GoalResponse toGoalResponse(Goal goal, List<Task> tasks) {
        int totalTasks = tasks.size();
        int completedTasks = (int) tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();

        return GoalResponse.builder()
                .id(goal.getId())
                .title(goal.getTitle())
                .description(goal.getDescription())
                .type(goal.getType())
                .deadline(goal.getDeadline())
                .organizationId(goal.getOrganizationId())
                .userId(goal.getUserId())
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .build();
    }
}
