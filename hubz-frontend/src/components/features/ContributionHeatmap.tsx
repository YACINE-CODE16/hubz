import { useState, useMemo } from 'react';
import { Flame, Calendar, TrendingUp, Activity } from 'lucide-react';
import Card from '../ui/Card';
import { cn } from '../../lib/utils';
import type { ActivityHeatmap, DailyActivity } from '../../types/analytics';

interface ContributionHeatmapProps {
  data: ActivityHeatmap | null;
  loading?: boolean;
  title?: string;
  showStats?: boolean;
  compact?: boolean;
}

const MONTHS = ['Jan', 'Fev', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Aout', 'Sep', 'Oct', 'Nov', 'Dec'];
const DAYS = ['Dim', 'Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam'];

export default function ContributionHeatmap({
  data,
  loading = false,
  title = 'Contributions',
  showStats = true,
  compact = false,
}: ContributionHeatmapProps) {
  const [hoveredDay, setHoveredDay] = useState<DailyActivity | null>(null);
  const [tooltipPosition, setTooltipPosition] = useState({ x: 0, y: 0 });

  // Group activities by week (7 rows, 52+ columns)
  const { weeks, monthLabels } = useMemo(() => {
    if (!data?.activities || data.activities.length === 0) {
      return { weeks: [], monthLabels: [] };
    }

    const activities = [...data.activities];
    const weeksArray: DailyActivity[][] = [];
    const labels: { month: string; weekIndex: number }[] = [];

    // Pad the beginning to start on Sunday
    const firstDate = new Date(activities[0].date);
    const firstDayOfWeek = firstDate.getDay();
    const paddedActivities: (DailyActivity | null)[] = [];

    // Add null padding for days before the first date
    for (let i = 0; i < firstDayOfWeek; i++) {
      paddedActivities.push(null);
    }

    // Add actual activities
    paddedActivities.push(...activities);

    // Group into weeks
    let currentWeek: (DailyActivity | null)[] = [];
    let currentMonth = -1;

    paddedActivities.forEach((activity, index) => {
      currentWeek.push(activity);

      // Check for month change (for labels)
      if (activity) {
        const date = new Date(activity.date);
        const month = date.getMonth();
        if (month !== currentMonth && date.getDate() <= 7) {
          labels.push({
            month: MONTHS[month],
            weekIndex: Math.floor(index / 7),
          });
          currentMonth = month;
        }
      }

      if (currentWeek.length === 7) {
        weeksArray.push(currentWeek.map(a => a || { date: '', count: 0, level: 0 }));
        currentWeek = [];
      }
    });

    // Push remaining days
    if (currentWeek.length > 0) {
      while (currentWeek.length < 7) {
        currentWeek.push({ date: '', count: 0, level: 0 });
      }
      weeksArray.push(currentWeek as DailyActivity[]);
    }

    return { weeks: weeksArray, monthLabels: labels };
  }, [data?.activities]);

  const handleMouseEnter = (
    activity: DailyActivity,
    event: React.MouseEvent<HTMLDivElement>
  ) => {
    if (!activity.date) return;
    setHoveredDay(activity);
    const rect = event.currentTarget.getBoundingClientRect();
    setTooltipPosition({
      x: rect.left + rect.width / 2,
      y: rect.top - 10,
    });
  };

  const handleMouseLeave = () => {
    setHoveredDay(null);
  };

  const getLevelColor = (level: number) => {
    switch (level) {
      case 0:
        return 'bg-gray-100 dark:bg-gray-800';
      case 1:
        return 'bg-green-200 dark:bg-green-900';
      case 2:
        return 'bg-green-300 dark:bg-green-700';
      case 3:
        return 'bg-green-400 dark:bg-green-600';
      case 4:
        return 'bg-green-500 dark:bg-green-500';
      default:
        return 'bg-gray-100 dark:bg-gray-800';
    }
  };

  const formatDate = (dateStr: string) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  };

  if (loading) {
    return (
      <Card className="p-4 sm:p-6">
        <div className="flex items-center justify-center h-40">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-accent border-t-transparent" />
        </div>
      </Card>
    );
  }

  if (!data) {
    return null;
  }

  return (
    <Card className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-3 flex flex-col gap-1 sm:mb-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-2">
          <Activity className="h-5 w-5 text-gray-400" />
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 sm:text-base">{title}</h3>
        </div>
        {data.totalContributions > 0 && (
          <span className="text-xs text-gray-500 dark:text-gray-400 sm:text-sm">
            {data.totalContributions} contributions dans l'annee
          </span>
        )}
      </div>

      {/* Stats Row */}
      {showStats && (
        <div className="mb-3 grid grid-cols-2 gap-2 sm:mb-4 sm:gap-4 sm:grid-cols-4">
          <StatItem
            icon={<Flame className="h-4 w-4 text-orange-500" />}
            label="Serie actuelle"
            value={data.currentStreak.toString()}
            suffix="jours"
          />
          <StatItem
            icon={<TrendingUp className="h-4 w-4 text-green-500" />}
            label="Meilleure serie"
            value={data.longestStreak.toString()}
            suffix="jours"
          />
          <StatItem
            icon={<Calendar className="h-4 w-4 text-blue-500" />}
            label="Jours actifs"
            value={data.activeDays.toString()}
            suffix={`/ ${data.totalDays}`}
          />
          <StatItem
            icon={<Activity className="h-4 w-4 text-purple-500" />}
            label="Moyenne"
            value={data.averagePerDay.toFixed(1)}
            suffix="/ jour"
          />
        </div>
      )}

      {/* Heatmap Grid */}
      <div className="relative overflow-x-auto -mx-2 px-2 sm:mx-0 sm:px-0">
        {/* Month Labels */}
        <div className="mb-2 flex pl-4 sm:pl-8" style={{ minWidth: `${weeks.length * 12}px` }}>
          {monthLabels.map((label, index) => (
            <div
              key={index}
              className="text-[9px] text-gray-500 dark:text-gray-400 sm:text-xs"
              style={{
                position: 'absolute',
                left: `${16 + label.weekIndex * 12}px`,
              }}
            >
              {label.month}
            </div>
          ))}
        </div>

        <div className="flex gap-[1px] pt-4 sm:gap-[2px]">
          {/* Day Labels - hidden on mobile for space */}
          <div className="hidden flex-col gap-[2px] pr-2 sm:flex">
            {DAYS.map((day, index) => (
              <div
                key={day}
                className={cn(
                  'h-3 text-[10px] text-gray-400 flex items-center',
                  index % 2 === 1 ? 'invisible' : ''
                )}
              >
                {day}
              </div>
            ))}
          </div>

          {/* Weeks Grid */}
          <div className="flex gap-[1px] sm:gap-[2px]">
            {weeks.map((week, weekIndex) => (
              <div key={weekIndex} className="flex flex-col gap-[1px] sm:gap-[2px]">
                {week.map((day, dayIndex) => (
                  <div
                    key={`${weekIndex}-${dayIndex}`}
                    className={cn(
                      'h-[10px] w-[10px] rounded-[2px] transition-all cursor-pointer sm:h-3 sm:w-3 sm:rounded-sm',
                      getLevelColor(day.level),
                      day.date && 'hover:ring-2 hover:ring-accent hover:ring-offset-1 dark:hover:ring-offset-dark-card'
                    )}
                    onMouseEnter={(e) => handleMouseEnter(day, e)}
                    onMouseLeave={handleMouseLeave}
                  />
                ))}
              </div>
            ))}
          </div>
        </div>

        {/* Legend */}
        <div className="mt-3 flex flex-col gap-1 sm:mt-4 sm:flex-row sm:items-center sm:justify-between">
          {data.mostActiveDay && (
            <span className="hidden text-xs text-gray-500 dark:text-gray-400 sm:block">
              Jour le plus actif: <span className="font-medium">{data.mostActiveDay}</span>
            </span>
          )}
          <div className="flex items-center gap-1 text-[10px] text-gray-500 dark:text-gray-400 sm:ml-auto sm:text-xs">
            <span>Moins</span>
            <div className="flex gap-[1px] sm:gap-[2px]">
              {[0, 1, 2, 3, 4].map((level) => (
                <div
                  key={level}
                  className={cn('h-[10px] w-[10px] rounded-[2px] sm:h-3 sm:w-3 sm:rounded-sm', getLevelColor(level))}
                />
              ))}
            </div>
            <span>Plus</span>
          </div>
        </div>
      </div>

      {/* Tooltip */}
      {hoveredDay && hoveredDay.date && (
        <div
          className="fixed z-50 pointer-events-none"
          style={{
            left: tooltipPosition.x,
            top: tooltipPosition.y,
            transform: 'translate(-50%, -100%)',
          }}
        >
          <div className="rounded-lg bg-gray-900 dark:bg-gray-700 px-3 py-2 text-xs text-white shadow-lg">
            <div className="font-medium">
              {hoveredDay.count} contribution{hoveredDay.count !== 1 ? 's' : ''}
            </div>
            <div className="text-gray-300 dark:text-gray-400 capitalize">
              {formatDate(hoveredDay.date)}
            </div>
          </div>
        </div>
      )}
    </Card>
  );
}

interface StatItemProps {
  icon: React.ReactNode;
  label: string;
  value: string;
  suffix?: string;
}

function StatItem({ icon, label, value, suffix }: StatItemProps) {
  return (
    <div className="flex items-center gap-1.5 rounded-lg bg-gray-50 dark:bg-gray-800/50 p-1.5 sm:gap-2 sm:p-2">
      <div className="hidden sm:block">{icon}</div>
      <div className="min-w-0">
        <div className="truncate text-[10px] text-gray-500 dark:text-gray-400 sm:text-xs">{label}</div>
        <div className="flex items-baseline gap-1">
          <span className="text-base font-bold text-gray-900 dark:text-gray-100 sm:text-lg">{value}</span>
          {suffix && (
            <span className="text-[10px] text-gray-500 dark:text-gray-400 sm:text-xs">{suffix}</span>
          )}
        </div>
      </div>
    </div>
  );
}
