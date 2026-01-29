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
