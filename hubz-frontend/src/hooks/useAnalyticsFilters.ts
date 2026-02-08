import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import type { TaskStatus, TaskPriority } from '../types/task';
import type { AnalyticsFilters, DateRangePreset } from '../types/analyticsFilters';
import { DEFAULT_ANALYTICS_FILTERS } from '../types/analyticsFilters';

/**
 * Compute start/end dates for a given preset.
 */
function computePresetDates(preset: DateRangePreset): { startDate?: string; endDate?: string } {
  const today = new Date();
  const formatDate = (d: Date): string => d.toISOString().split('T')[0];

  switch (preset) {
    case 'this_week': {
      const dayOfWeek = today.getDay();
      const monday = new Date(today);
      monday.setDate(today.getDate() - ((dayOfWeek + 6) % 7));
      return { startDate: formatDate(monday), endDate: formatDate(today) };
    }
    case 'this_month': {
      const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
      return { startDate: formatDate(firstDay), endDate: formatDate(today) };
    }
    case 'last_30_days': {
      const start = new Date(today);
      start.setDate(today.getDate() - 30);
      return { startDate: formatDate(start), endDate: formatDate(today) };
    }
    case 'last_90_days': {
      const start = new Date(today);
      start.setDate(today.getDate() - 90);
      return { startDate: formatDate(start), endDate: formatDate(today) };
    }
    case 'custom':
      // Custom: dates are set explicitly, not computed from preset
      return {};
    default:
      return {};
  }
}

const VALID_STATUSES: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];
const VALID_PRIORITIES: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
const VALID_PRESETS: DateRangePreset[] = ['this_week', 'this_month', 'last_30_days', 'last_90_days', 'custom'];

/**
 * Custom hook to manage analytics filters with URL search param persistence.
 *
 * Returns the current filters, a setter to update individual filter fields,
 * a reset function, and computed date bounds (from presets).
 */
export function useAnalyticsFilters() {
  const [searchParams, setSearchParams] = useSearchParams();

  // Parse filters from URL search params
  const filters: AnalyticsFilters = useMemo(() => {
    const presetParam = searchParams.get('dateRange');
    const preset: DateRangePreset =
      presetParam && VALID_PRESETS.includes(presetParam as DateRangePreset)
        ? (presetParam as DateRangePreset)
        : DEFAULT_ANALYTICS_FILTERS.dateRangePreset;

    const startDate = searchParams.get('startDate') || undefined;
    const endDate = searchParams.get('endDate') || undefined;

    const memberIdsParam = searchParams.get('memberIds');
    const memberIds = memberIdsParam ? memberIdsParam.split(',').filter(Boolean) : [];

    const statusesParam = searchParams.get('statuses');
    const statuses: TaskStatus[] = statusesParam
      ? (statusesParam.split(',').filter((s) => VALID_STATUSES.includes(s as TaskStatus)) as TaskStatus[])
      : [];

    const prioritiesParam = searchParams.get('priorities');
    const priorities: TaskPriority[] = prioritiesParam
      ? (prioritiesParam.split(',').filter((p) => VALID_PRIORITIES.includes(p as TaskPriority)) as TaskPriority[])
      : [];

    return {
      dateRangePreset: preset,
      startDate,
      endDate,
      memberIds,
      statuses,
      priorities,
    };
  }, [searchParams]);

  // Compute effective date range (from preset or custom values)
  const effectiveDateRange = useMemo(() => {
    if (filters.dateRangePreset === 'custom') {
      return {
        startDate: filters.startDate,
        endDate: filters.endDate,
      };
    }
    return computePresetDates(filters.dateRangePreset);
  }, [filters.dateRangePreset, filters.startDate, filters.endDate]);

  // Write filters to URL search params
  const setFilters = useCallback(
    (newFilters: Partial<AnalyticsFilters>) => {
      setSearchParams(
        (prev) => {
          const updated = new URLSearchParams(prev);

          if (newFilters.dateRangePreset !== undefined) {
            if (newFilters.dateRangePreset === DEFAULT_ANALYTICS_FILTERS.dateRangePreset) {
              updated.delete('dateRange');
            } else {
              updated.set('dateRange', newFilters.dateRangePreset);
            }
          }

          if (newFilters.startDate !== undefined) {
            if (newFilters.startDate) {
              updated.set('startDate', newFilters.startDate);
            } else {
              updated.delete('startDate');
            }
          }

          if (newFilters.endDate !== undefined) {
            if (newFilters.endDate) {
              updated.set('endDate', newFilters.endDate);
            } else {
              updated.delete('endDate');
            }
          }

          if (newFilters.memberIds !== undefined) {
            if (newFilters.memberIds.length > 0) {
              updated.set('memberIds', newFilters.memberIds.join(','));
            } else {
              updated.delete('memberIds');
            }
          }

          if (newFilters.statuses !== undefined) {
            if (newFilters.statuses.length > 0) {
              updated.set('statuses', newFilters.statuses.join(','));
            } else {
              updated.delete('statuses');
            }
          }

          if (newFilters.priorities !== undefined) {
            if (newFilters.priorities.length > 0) {
              updated.set('priorities', newFilters.priorities.join(','));
            } else {
              updated.delete('priorities');
            }
          }

          return updated;
        },
        { replace: true },
      );
    },
    [setSearchParams],
  );

  // Reset all filters to defaults
  const resetFilters = useCallback(() => {
    setSearchParams(
      (prev) => {
        const updated = new URLSearchParams(prev);
        updated.delete('dateRange');
        updated.delete('startDate');
        updated.delete('endDate');
        updated.delete('memberIds');
        updated.delete('statuses');
        updated.delete('priorities');
        return updated;
      },
      { replace: true },
    );
  }, [setSearchParams]);

  return {
    filters,
    effectiveDateRange,
    setFilters,
    resetFilters,
  };
}
