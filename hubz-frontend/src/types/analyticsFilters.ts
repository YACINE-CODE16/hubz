import type { TaskStatus, TaskPriority } from './task';

/**
 * Preset date range options for analytics filters.
 */
export type DateRangePreset = 'this_week' | 'this_month' | 'last_30_days' | 'last_90_days' | 'custom';

/**
 * Dynamic filters for analytics dashboards.
 * All fields are optional -- when undefined, no filtering is applied.
 */
export interface AnalyticsFilters {
  dateRangePreset: DateRangePreset;
  startDate?: string; // ISO date string (YYYY-MM-DD)
  endDate?: string;   // ISO date string (YYYY-MM-DD)
  memberIds: string[];
  statuses: TaskStatus[];
  priorities: TaskPriority[];
}

/**
 * Default (empty) filters -- no filtering applied.
 */
export const DEFAULT_ANALYTICS_FILTERS: AnalyticsFilters = {
  dateRangePreset: 'last_30_days',
  startDate: undefined,
  endDate: undefined,
  memberIds: [],
  statuses: [],
  priorities: [],
};

/**
 * Build URL query params from analytics filters, omitting empty/default values.
 */
export function filtersToQueryParams(filters: AnalyticsFilters): Record<string, string | string[]> {
  const params: Record<string, string | string[]> = {};

  if (filters.startDate) {
    params.startDate = filters.startDate;
  }
  if (filters.endDate) {
    params.endDate = filters.endDate;
  }
  if (filters.memberIds.length > 0) {
    params.memberIds = filters.memberIds;
  }
  if (filters.statuses.length > 0) {
    params.statuses = filters.statuses;
  }
  if (filters.priorities.length > 0) {
    params.priorities = filters.priorities;
  }

  return params;
}

/**
 * Count how many active (non-default) filters are applied.
 */
export function countActiveFilters(filters: AnalyticsFilters): number {
  let count = 0;
  if (filters.startDate || filters.endDate) count++;
  if (filters.memberIds.length > 0) count++;
  if (filters.statuses.length > 0) count++;
  if (filters.priorities.length > 0) count++;
  return count;
}
