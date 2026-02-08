import api from './api';
import type {
  TaskAnalytics,
  MemberAnalytics,
  GoalAnalytics,
  HabitAnalytics,
  OrganizationAnalytics,
  ProductivityStats,
  CalendarAnalytics,
  ActivityHeatmap,
} from '../types/analytics';
import type { AnalyticsFilters } from '../types/analyticsFilters';

/**
 * Build URL query string from analytics filters.
 * Only includes non-empty filter values.
 */
function buildFilterParams(filters?: AnalyticsFilters, effectiveDateRange?: { startDate?: string; endDate?: string }): string {
  const params = new URLSearchParams();

  if (effectiveDateRange?.startDate) {
    params.append('startDate', effectiveDateRange.startDate);
  }
  if (effectiveDateRange?.endDate) {
    params.append('endDate', effectiveDateRange.endDate);
  }

  if (filters?.memberIds && filters.memberIds.length > 0) {
    filters.memberIds.forEach((id) => params.append('memberIds', id));
  }
  if (filters?.statuses && filters.statuses.length > 0) {
    filters.statuses.forEach((s) => params.append('statuses', s));
  }
  if (filters?.priorities && filters.priorities.length > 0) {
    filters.priorities.forEach((p) => params.append('priorities', p));
  }

  const queryString = params.toString();
  return queryString ? `?${queryString}` : '';
}

export const analyticsService = {
  /**
   * Get task analytics for an organization
   */
  getTaskAnalytics: async (
    orgId: string,
    filters?: AnalyticsFilters,
    effectiveDateRange?: { startDate?: string; endDate?: string },
  ): Promise<TaskAnalytics> => {
    const qs = buildFilterParams(filters, effectiveDateRange);
    const response = await api.get<TaskAnalytics>(`/organizations/${orgId}/analytics/tasks${qs}`);
    return response.data;
  },

  /**
   * Get member analytics for an organization
   */
  getMemberAnalytics: async (
    orgId: string,
    filters?: AnalyticsFilters,
    effectiveDateRange?: { startDate?: string; endDate?: string },
  ): Promise<MemberAnalytics> => {
    const qs = buildFilterParams(filters, effectiveDateRange);
    const response = await api.get<MemberAnalytics>(`/organizations/${orgId}/analytics/members${qs}`);
    return response.data;
  },

  /**
   * Get goal analytics for an organization
   */
  getGoalAnalytics: async (
    orgId: string,
    filters?: AnalyticsFilters,
    effectiveDateRange?: { startDate?: string; endDate?: string },
  ): Promise<GoalAnalytics> => {
    const qs = buildFilterParams(filters, effectiveDateRange);
    const response = await api.get<GoalAnalytics>(`/organizations/${orgId}/analytics/goals${qs}`);
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

  /**
   * Get productivity statistics for the current user
   */
  getProductivityStats: async (): Promise<ProductivityStats> => {
    const response = await api.get<ProductivityStats>('/users/me/productivity-stats');
    return response.data;
  },

  /**
   * Get calendar and time analytics for the current user
   * @param startDate - Start of the analysis period (ISO date string)
   * @param endDate - End of the analysis period (ISO date string)
   */
  getCalendarAnalytics: async (startDate?: string, endDate?: string): Promise<CalendarAnalytics> => {
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    const queryString = params.toString();
    const url = `/users/me/calendar-analytics${queryString ? `?${queryString}` : ''}`;
    const response = await api.get<CalendarAnalytics>(url);
    return response.data;
  },

  /**
   * Get activity heatmap for the current user (12 months)
   */
  getUserActivityHeatmap: async (): Promise<ActivityHeatmap> => {
    const response = await api.get<ActivityHeatmap>('/users/me/activity-heatmap');
    return response.data;
  },

  /**
   * Get aggregated activity heatmap for an organization
   */
  getOrganizationActivityHeatmap: async (orgId: string): Promise<ActivityHeatmap> => {
    const response = await api.get<ActivityHeatmap>(`/organizations/${orgId}/activity-heatmap`);
    return response.data;
  },

  /**
   * Get activity heatmap for a specific member in an organization
   */
  getMemberActivityHeatmap: async (orgId: string, memberId: string): Promise<ActivityHeatmap> => {
    const response = await api.get<ActivityHeatmap>(`/organizations/${orgId}/members/${memberId}/activity-heatmap`);
    return response.data;
  },
};
