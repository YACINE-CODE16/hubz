package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalDashboardResponse {

    // Summary statistics
    private DashboardStats stats;

    // Today's tasks (assigned to user, not completed, due today or overdue)
    private List<TaskResponse> todayTasks;

    // Today's habits with their completion status
    private List<HabitWithStatusResponse> todayHabits;

    // Upcoming events (next 7 days)
    private List<EventResponse> upcomingEvents;

    // Goals with progress
    private List<GoalResponse> goals;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStats {
        private int totalGoals;
        private int completedGoals;
        private int totalTasks;
        private int completedTasks;
        private int overdueTasks;
        private int todayTasksCount;
        private int totalHabits;
        private int completedHabitsToday;
        private int currentStreak;
        private int upcomingEventsCount;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HabitWithStatusResponse {
        private HabitResponse habit;
        private boolean completedToday;
        private int currentStreak;
        private int completedLast7Days;
    }
}
