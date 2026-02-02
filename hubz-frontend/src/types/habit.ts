export type HabitFrequency = 'DAILY' | 'WEEKLY';

export interface Habit {
  id: string;
  name: string;
  icon: string;
  frequency: HabitFrequency;
  userId: string;
  createdAt: string;
  updatedAt: string;
}

export interface HabitLog {
  id: string;
  habitId: string;
  date: string;
  completed: boolean;
  notes?: string;
  duration?: number;
  createdAt: string;
}

export interface CreateHabitRequest {
  name: string;
  icon: string;
  frequency: HabitFrequency;
}

export interface UpdateHabitRequest {
  name?: string;
  icon?: string;
  frequency?: HabitFrequency;
}

export interface LogHabitRequest {
  date: string;
  completed: boolean;
  notes?: string;
  duration?: number;
}

// Analytics Types
export interface HabitStats {
  habitId: string;
  habitName: string;
  habitIcon: string;
  frequency: string;
  currentStreak: number;
  longestStreak: number;
  completionRate: number;
  totalCompletions: number;
  lastCompletedDate: string | null;
}

export interface HeatmapData {
  date: string;
  completedCount: number;
  totalHabits: number;
  completionRate: number;
}

export interface TrendData {
  date: string;
  completionRate: number;
  completed: number;
  total: number;
}

export interface HabitAnalytics {
  totalHabits: number;
  dailyCompletionRate: number;
  weeklyCompletionRate: number;
  monthlyCompletionRate: number;
  longestStreak: number;
  currentStreak: number;
  bestStreakHabitName: string;
  habitStats: HabitStats[];
  completionHeatmap: HeatmapData[];
  completionByDayOfWeek: Record<string, number>;
  last30DaysTrend: TrendData[];
  last90DaysTrend: TrendData[];
}
