import api from './api';
import type {
  TaskAnalytics,
  MemberAnalytics,
  GoalAnalytics,
  HabitAnalytics,
  OrganizationAnalytics,
} from '../types/analytics';

export const analyticsService = {
  /**
   * Get task analytics for an organization
   */
  getTaskAnalytics: async (orgId: string): Promise<TaskAnalytics> => {
    const response = await api.get<TaskAnalytics>(`/organizations/${orgId}/analytics/tasks`);
    return response.data;
  },

  /**
   * Get member analytics for an organization
   */
  getMemberAnalytics: async (orgId: string): Promise<MemberAnalytics> => {
    const response = await api.get<MemberAnalytics>(`/organizations/${orgId}/analytics/members`);
    return response.data;
  },

  /**
   * Get goal analytics for an organization
   */
  getGoalAnalytics: async (orgId: string): Promise<GoalAnalytics> => {
    const response = await api.get<GoalAnalytics>(`/organizations/${orgId}/analytics/goals`);
    return response.data;
  },

  /**
   * Get overall organization analytics
   */
  getOrganizationAnalytics: async (orgId: string): Promise<OrganizationAnalytics> => {
    const response = await api.get<OrganizationAnalytics>(`/organizations/${orgId}/analytics`);
    return response.data;
  },

  /**
   * Get habit analytics for the current user
   */
  getHabitAnalytics: async (): Promise<HabitAnalytics> => {
    const response = await api.get<HabitAnalytics>('/users/me/analytics/habits');
    return response.data;
  },
};
