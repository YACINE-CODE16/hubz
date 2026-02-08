import { useState, useRef, useEffect } from 'react';
import {
  Calendar,
  Filter,
  Users,
  CheckCircle2,
  AlertTriangle,
  X,
  RotateCcw,
  ChevronDown,
} from 'lucide-react';
import { cn } from '../../lib/utils';
import type { TaskStatus, TaskPriority } from '../../types/task';
import type { AnalyticsFilters, DateRangePreset } from '../../types/analyticsFilters';
import { countActiveFilters } from '../../types/analyticsFilters';
import type { Member } from '../../types/organization';

interface DashboardFiltersProps {
  filters: AnalyticsFilters;
  onFiltersChange: (filters: Partial<AnalyticsFilters>) => void;
  onReset: () => void;
  members: Member[];
}

const DATE_RANGE_OPTIONS: { value: DateRangePreset; label: string }[] = [
  { value: 'this_week', label: 'Cette semaine' },
  { value: 'this_month', label: 'Ce mois' },
  { value: 'last_30_days', label: '30 derniers jours' },
  { value: 'last_90_days', label: '90 derniers jours' },
  { value: 'custom', label: 'Personnalise' },
];

const STATUS_OPTIONS: { value: TaskStatus; label: string; color: string }[] = [
  { value: 'TODO', label: 'A faire', color: 'bg-gray-400' },
  { value: 'IN_PROGRESS', label: 'En cours', color: 'bg-blue-500' },
  { value: 'DONE', label: 'Termine', color: 'bg-green-500' },
];

const PRIORITY_OPTIONS: { value: TaskPriority; label: string; color: string }[] = [
  { value: 'LOW', label: 'Basse', color: 'bg-gray-400' },
  { value: 'MEDIUM', label: 'Moyenne', color: 'bg-yellow-500' },
  { value: 'HIGH', label: 'Haute', color: 'bg-orange-500' },
  { value: 'URGENT', label: 'Urgente', color: 'bg-red-500' },
];

export default function DashboardFilters({
  filters,
  onFiltersChange,
  onReset,
  members,
}: DashboardFiltersProps) {
  const [showMemberDropdown, setShowMemberDropdown] = useState(false);
  const memberDropdownRef = useRef<HTMLDivElement>(null);
  const activeCount = countActiveFilters(filters);

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (memberDropdownRef.current && !memberDropdownRef.current.contains(event.target as Node)) {
        setShowMemberDropdown(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const toggleStatus = (status: TaskStatus) => {
    const current = filters.statuses;
    const updated = current.includes(status)
      ? current.filter((s) => s !== status)
      : [...current, status];
    onFiltersChange({ statuses: updated });
  };

  const togglePriority = (priority: TaskPriority) => {
    const current = filters.priorities;
    const updated = current.includes(priority)
      ? current.filter((p) => p !== priority)
      : [...current, priority];
    onFiltersChange({ priorities: updated });
  };

  const toggleMember = (memberId: string) => {
    const current = filters.memberIds;
    const updated = current.includes(memberId)
      ? current.filter((id) => id !== memberId)
      : [...current, memberId];
    onFiltersChange({ memberIds: updated });
  };

  return (
    <div className="rounded-xl border border-gray-200/50 bg-white/70 p-4 backdrop-blur-md shadow-sm dark:border-white/10 dark:bg-white/5">
      <div className="flex flex-wrap items-center gap-3">
        {/* Filter icon and label */}
        <div className="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
          <Filter className="h-4 w-4" />
          <span>Filtres</span>
          {activeCount > 0 && (
            <span className="flex h-5 w-5 items-center justify-center rounded-full bg-accent text-xs font-bold text-white">
              {activeCount}
            </span>
          )}
        </div>

        <div className="h-6 w-px bg-gray-200 dark:bg-gray-700" />

        {/* Date Range Selector */}
        <div className="flex items-center gap-2">
          <Calendar className="h-4 w-4 text-gray-500 dark:text-gray-400" />
          <select
            value={filters.dateRangePreset}
            onChange={(e) => {
              const preset = e.target.value as DateRangePreset;
              if (preset !== 'custom') {
                onFiltersChange({ dateRangePreset: preset, startDate: undefined, endDate: undefined });
              } else {
                onFiltersChange({ dateRangePreset: preset });
              }
            }}
            className="rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm text-gray-700 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-300"
          >
            {DATE_RANGE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          {filters.dateRangePreset === 'custom' && (
            <div className="flex items-center gap-1">
              <input
                type="date"
                value={filters.startDate || ''}
                onChange={(e) => onFiltersChange({ startDate: e.target.value || undefined })}
                className="rounded-lg border border-gray-200 bg-white px-2 py-1.5 text-sm text-gray-700 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-300"
              />
              <span className="text-xs text-gray-400">-</span>
              <input
                type="date"
                value={filters.endDate || ''}
                onChange={(e) => onFiltersChange({ endDate: e.target.value || undefined })}
                className="rounded-lg border border-gray-200 bg-white px-2 py-1.5 text-sm text-gray-700 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-300"
              />
            </div>
          )}
        </div>

        <div className="h-6 w-px bg-gray-200 dark:bg-gray-700" />

        {/* Status Filter */}
        <div className="flex items-center gap-1.5">
          <CheckCircle2 className="h-4 w-4 text-gray-500 dark:text-gray-400" />
          {STATUS_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              onClick={() => toggleStatus(opt.value)}
              className={cn(
                'flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium transition-colors',
                filters.statuses.includes(opt.value)
                  ? 'bg-accent text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-400 dark:hover:bg-gray-600',
              )}
            >
              <span className={cn('h-1.5 w-1.5 rounded-full', opt.color)} />
              {opt.label}
            </button>
          ))}
        </div>

        <div className="h-6 w-px bg-gray-200 dark:bg-gray-700" />

        {/* Priority Filter */}
        <div className="flex items-center gap-1.5">
          <AlertTriangle className="h-4 w-4 text-gray-500 dark:text-gray-400" />
          {PRIORITY_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              onClick={() => togglePriority(opt.value)}
              className={cn(
                'flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium transition-colors',
                filters.priorities.includes(opt.value)
                  ? 'bg-accent text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-400 dark:hover:bg-gray-600',
              )}
            >
              <span className={cn('h-1.5 w-1.5 rounded-full', opt.color)} />
              {opt.label}
            </button>
          ))}
        </div>

        <div className="h-6 w-px bg-gray-200 dark:bg-gray-700" />

        {/* Member Filter (multi-select dropdown) */}
        <div className="relative" ref={memberDropdownRef}>
          <button
            onClick={() => setShowMemberDropdown(!showMemberDropdown)}
            className={cn(
              'flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-sm transition-colors',
              filters.memberIds.length > 0
                ? 'border-accent bg-accent/10 text-accent'
                : 'border-gray-200 bg-white text-gray-600 hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-400 dark:hover:bg-gray-700',
            )}
          >
            <Users className="h-4 w-4" />
            <span>
              {filters.memberIds.length > 0
                ? `${filters.memberIds.length} membre${filters.memberIds.length > 1 ? 's' : ''}`
                : 'Membres'}
            </span>
            <ChevronDown className="h-3 w-3" />
          </button>

          {showMemberDropdown && (
            <div className="absolute left-0 top-full z-50 mt-1 max-h-60 w-64 overflow-auto rounded-xl border border-gray-200 bg-white shadow-lg dark:border-gray-700 dark:bg-gray-800">
              {members.length === 0 ? (
                <div className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">
                  Aucun membre
                </div>
              ) : (
                members.map((member) => (
                  <button
                    key={member.userId}
                    onClick={() => toggleMember(member.userId)}
                    className="flex w-full items-center gap-3 px-4 py-2 text-left text-sm transition-colors hover:bg-gray-50 dark:hover:bg-gray-700"
                  >
                    <div
                      className={cn(
                        'flex h-5 w-5 items-center justify-center rounded border transition-colors',
                        filters.memberIds.includes(member.userId)
                          ? 'border-accent bg-accent text-white'
                          : 'border-gray-300 dark:border-gray-600',
                      )}
                    >
                      {filters.memberIds.includes(member.userId) && (
                        <svg className="h-3 w-3" viewBox="0 0 12 12" fill="none">
                          <path d="M2 6l3 3 5-5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                        </svg>
                      )}
                    </div>
                    <div className="flex-1 truncate">
                      <span className="font-medium text-gray-900 dark:text-gray-100">
                        {member.firstName} {member.lastName}
                      </span>
                    </div>
                  </button>
                ))
              )}
            </div>
          )}
        </div>

        {/* Reset Button */}
        {activeCount > 0 && (
          <>
            <div className="h-6 w-px bg-gray-200 dark:bg-gray-700" />
            <button
              onClick={onReset}
              className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-gray-300"
            >
              <RotateCcw className="h-3.5 w-3.5" />
              Reinitialiser
            </button>
          </>
        )}
      </div>

      {/* Active Filters Summary */}
      {activeCount > 0 && (
        <div className="mt-3 flex flex-wrap items-center gap-2 border-t border-gray-100 pt-3 dark:border-gray-700">
          <span className="text-xs text-gray-500 dark:text-gray-400">Filtres actifs:</span>
          {(filters.startDate || filters.endDate) && (
            <FilterChip
              label={
                filters.dateRangePreset !== 'custom'
                  ? DATE_RANGE_OPTIONS.find((o) => o.value === filters.dateRangePreset)?.label || ''
                  : `${filters.startDate || '...'} - ${filters.endDate || '...'}`
              }
              onRemove={() =>
                onFiltersChange({
                  dateRangePreset: 'last_30_days',
                  startDate: undefined,
                  endDate: undefined,
                })
              }
            />
          )}
          {filters.statuses.map((s) => {
            const opt = STATUS_OPTIONS.find((o) => o.value === s);
            return (
              <FilterChip
                key={s}
                label={opt?.label || s}
                onRemove={() => toggleStatus(s)}
              />
            );
          })}
          {filters.priorities.map((p) => {
            const opt = PRIORITY_OPTIONS.find((o) => o.value === p);
            return (
              <FilterChip
                key={p}
                label={opt?.label || p}
                onRemove={() => togglePriority(p)}
              />
            );
          })}
          {filters.memberIds.map((id) => {
            const member = members.find((m) => m.userId === id);
            return (
              <FilterChip
                key={id}
                label={member ? `${member.firstName} ${member.lastName}` : id}
                onRemove={() => toggleMember(id)}
              />
            );
          })}
        </div>
      )}
    </div>
  );
}

function FilterChip({ label, onRemove }: { label: string; onRemove: () => void }) {
  return (
    <span className="flex items-center gap-1 rounded-full bg-accent/10 px-2.5 py-0.5 text-xs font-medium text-accent">
      {label}
      <button
        onClick={onRemove}
        className="ml-0.5 rounded-full p-0.5 transition-colors hover:bg-accent/20"
      >
        <X className="h-3 w-3" />
      </button>
    </span>
  );
}
