import api from './api';
import type {
  Goal,
  GoalAnalytics,
  CreateGoalRequest,
  UpdateGoalRequest,
} from '../types/goal';
import type { Task } from '../types/task';

export const goalService = {
  async getByOrganization(organizationId: string): Promise<Goal[]> {
    const response = await api.get<Goal[]>(`/organizations/${organizationId}/goals`);
    return response.data;
  },

  async createOrganizationGoal(organizationId: string, data: CreateGoalRequest): Promise<Goal> {
    const response = await api.post<Goal>(`/organizations/${organizationId}/goals`, data);
    return response.data;
  },

  async getPersonalGoals(): Promise<Goal[]> {
    const response = await api.get<Goal[]>('/users/me/goals');
    return response.data;
  },

  async createPersonalGoal(data: CreateGoalRequest): Promise<Goal> {
    const response = await api.post<Goal>('/users/me/goals', data);
    return response.data;
  },

  async update(id: string, data: UpdateGoalRequest): Promise<Goal> {
    const response = await api.put<Goal>(`/goals/${id}`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/goals/${id}`);
  },

  async getById(id: string): Promise<Goal> {
    const response = await api.get<Goal>(`/goals/${id}`);
    return response.data;
  },

  async getTasksByGoal(goalId: string): Promise<Task[]> {
    const response = await api.get<Task[]>(`/goals/${goalId}/tasks`);
    return response.data;
  },

  async getPersonalAnalytics(): Promise<GoalAnalytics> {
    const response = await api.get<GoalAnalytics>('/users/me/goals/analytics');
    return response.data;
  },

  async getOrganizationAnalytics(organizationId: string): Promise<GoalAnalytics> {
    const response = await api.get<GoalAnalytics>(`/organizations/${organizationId}/goals/analytics`);
    return response.data;
  },
};
