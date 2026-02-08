import type { Task } from './task';
import type { Event } from './event';
import type { Goal } from './goal';
import type { Habit } from './habit';

export interface DashboardStats {
  totalGoals: number;
  completedGoals: number;
  totalTasks: number;
  completedTasks: number;
  overdueTasks: number;
  todayTasksCount: number;
  totalHabits: number;
  completedHabitsToday: number;
  currentStreak: number;
  upcomingEventsCount: number;
}

export interface HabitWithStatus {
  habit: Habit;
  completedToday: boolean;
  currentStreak: number;
  completedLast7Days: number;
}

export interface PersonalDashboard {
  stats: DashboardStats;
  todayTasks: Task[];
  todayHabits: HabitWithStatus[];
  upcomingEvents: Event[];
  goals: Goal[];
}
