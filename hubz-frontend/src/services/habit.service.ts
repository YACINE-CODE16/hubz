import api from './api';
import type {
  Habit,
  HabitLog,
  CreateHabitRequest,
  UpdateHabitRequest,
  LogHabitRequest,
} from '../types/habit';

export const habitService = {
  async getUserHabits(): Promise<Habit[]> {
    const response = await api.get<Habit[]>('/users/me/habits');
    return response.data;
  },

  async create(data: CreateHabitRequest): Promise<Habit> {
    const response = await api.post<Habit>('/users/me/habits', data);
    return response.data;
  },

  async update(id: string, data: UpdateHabitRequest): Promise<Habit> {
    const response = await api.put<Habit>(`/habits/${id}`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/habits/${id}`);
  },

  async logHabit(id: string, data: LogHabitRequest): Promise<HabitLog> {
    const response = await api.post<HabitLog>(`/habits/${id}/log`, data);
    return response.data;
  },

  async getHabitLogs(id: string): Promise<HabitLog[]> {
    const response = await api.get<HabitLog[]>(`/habits/${id}/logs`);
    return response.data;
  },
};
