import { useState, useEffect } from 'react';
import {
  TrendingUp,
  TrendingDown,
  Target,
  Calendar,
  Zap,
  Flame,
  Clock,
  Award,
  BarChart3,
  AlertCircle,
  ArrowUpRight,
  ArrowDownRight,
} from 'lucide-react';
import toast from 'react-hot-toast';
import Card from '../ui/Card';
import { analyticsService } from '../../services/analytics.service';
import type { ProductivityStats, DailyTaskCount } from '../../types/analytics';
import { cn } from '../../lib/utils';

interface Props {
  refreshKey?: number;
}

export default function ProductivityStatsSection({ refreshKey }: Props) {
  const [stats, setStats] = useState<ProductivityStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStats();
  }, [refreshKey]);

  const loadStats = async () => {
    try {
      setLoading(true);
      const data = await analyticsService.getProductivityStats();
      setStats(data);
    } catch (error) {
      toast.error('Erreur lors du chargement des statistiques');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Card className="p-4 sm:p-6">
        <div className="flex h-48 items-center justify-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-accent border-t-transparent" />
        </div>
      </Card>
    );
  }

  if (!stats) {
    return (
      <Card className="p-4 sm:p-6">
        <div className="flex h-48 flex-col items-center justify-center text-gray-500 dark:text-gray-400">
          <AlertCircle className="mb-2 h-8 w-8" />
          <p className="text-sm sm:text-base">Impossible de charger les statistiques</p>
        </div>
      </Card>
    );
  }

  return (
    <div className="space-y-4 sm:space-y-6">
      {/* Header with Insight */}
      <Card className="p-4 sm:p-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0 flex-1">
            <h3 className="flex items-center gap-2 text-base font-semibold text-gray-900 dark:text-gray-100 sm:text-lg">
              <BarChart3 className="h-5 w-5 flex-shrink-0 text-accent" />
              <span className="truncate">Statistiques de productivite</span>
            </h3>
            <p className="mt-1 text-xs text-gray-600 dark:text-gray-400 sm:mt-2 sm:text-sm">
              {stats.insight}
            </p>
          </div>
          <div className="flex justify-center sm:justify-end">
            <ProductivityScoreGauge score={stats.productivityScore} />
          </div>
        </div>
      </Card>

      {/* Summary Stats Grid */}
      <div className="grid grid-cols-2 gap-2 sm:gap-4 lg:grid-cols-4">
        <StatCard
          title="Taches cette semaine"
          value={stats.tasksCompletedThisWeek.toString()}
          subtitle={`sur ${stats.totalTasksThisWeek} assignees`}
          change={stats.weeklyChange}
          icon={<Target className="h-5 w-5" />}
          color="blue"
        />
        <StatCard
          title="Taches ce mois"
          value={stats.tasksCompletedThisMonth.toString()}
          subtitle={`sur ${stats.totalTasksThisMonth} assignees`}
          change={stats.monthlyChange}
          icon={<Calendar className="h-5 w-5" />}
          color="purple"
        />
        <StatCard
          title="Serie productive"
          value={stats.productiveStreak.toString()}
          subtitle={`Record: ${stats.longestProductiveStreak} jours`}
          icon={<Flame className="h-5 w-5" />}
          color="orange"
        />
        <StatCard
          title="Temps moyen"
          value={stats.averageCompletionTimeHours ? `${stats.averageCompletionTimeHours}h` : '-'}
          subtitle="par tache"
          icon={<Clock className="h-5 w-5" />}
          color="green"
        />
      </div>

      {/* Charts Row */}
      <div className="grid gap-4 sm:gap-6 lg:grid-cols-2">
        {/* Daily Tasks Chart */}
        <Card className="p-4 sm:p-6">
          <h4 className="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-gray-100 sm:mb-4 sm:text-base">
            <TrendingUp className="h-4 w-4 text-gray-400 sm:h-5 sm:w-5" />
            Taches completees (30 jours)
          </h4>
          <DailyTasksChart data={stats.dailyTasksCompleted} />
        </Card>

        {/* Priority Breakdown */}
        <Card className="p-4 sm:p-6">
          <h4 className="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-gray-100 sm:mb-4 sm:text-base">
            <Zap className="h-4 w-4 text-gray-400 sm:h-5 sm:w-5" />
            Repartition par priorite (ce mois)
          </h4>
          <PriorityBreakdown stats={stats} />
        </Card>
      </div>

      {/* Completion Rates and Additional Info */}
      <div className="grid gap-4 sm:gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {/* Completion Rates */}
        <Card className="p-4 sm:p-6">
          <h4 className="mb-3 text-sm font-semibold text-gray-900 dark:text-gray-100 sm:mb-4 sm:text-base">
            Taux de completion
          </h4>
          <div className="space-y-3 sm:space-y-4">
            <CompletionRateBar
              label="Cette semaine"
              rate={stats.weeklyCompletionRate}
              color="accent"
            />
            <CompletionRateBar
              label="Ce mois"
              rate={stats.monthlyCompletionRate}
              color="purple"
            />
          </div>
        </Card>

        {/* Most Productive Day */}
        <Card className="p-4 sm:p-6">
          <h4 className="mb-3 text-sm font-semibold text-gray-900 dark:text-gray-100 sm:mb-4 sm:text-base">
            Jour le plus productif
          </h4>
          <div className="flex h-20 items-center justify-center sm:h-24">
            {stats.mostProductiveDay ? (
              <div className="text-center">
                <Award className="mx-auto mb-1.5 h-8 w-8 text-yellow-500 sm:mb-2 sm:h-10 sm:w-10" />
                <p className="text-xl font-bold text-gray-900 dark:text-gray-100 sm:text-2xl">
                  {stats.mostProductiveDay}
                </p>
              </div>
            ) : (
              <p className="text-xs text-gray-500 dark:text-gray-400 sm:text-sm">
                Pas assez de donnees
              </p>
            )}
          </div>
        </Card>

        {/* Week vs Last Week Comparison */}
        <Card className="p-4 sm:p-6 sm:col-span-2 lg:col-span-1">
          <h4 className="mb-3 text-sm font-semibold text-gray-900 dark:text-gray-100 sm:mb-4 sm:text-base">
            Comparaison hebdomadaire
          </h4>
          <div className="flex h-20 items-center justify-center sm:h-24">
            <WeeklyComparison change={stats.weeklyChange} />
          </div>
        </Card>
      </div>
    </div>
  );
}

// Sub-components

interface ProductivityScoreGaugeProps {
  score: number;
}

function ProductivityScoreGauge({ score }: ProductivityScoreGaugeProps) {
  const getScoreColor = (score: number) => {
    if (score >= 80) return 'text-green-500';
    if (score >= 60) return 'text-blue-500';
    if (score >= 40) return 'text-yellow-500';
    return 'text-red-500';
  };

  const getScoreLabel = (score: number) => {
    if (score >= 80) return 'Excellent';
    if (score >= 60) return 'Bon';
    if (score >= 40) return 'Moyen';
    return 'A ameliorer';
  };

  const circumference = 2 * Math.PI * 40;
  const strokeDashoffset = circumference - (score / 100) * circumference;

  return (
    <div className="relative flex flex-col items-center">
      <svg className="h-20 w-20 -rotate-90 transform sm:h-24 sm:w-24" viewBox="0 0 96 96">
        {/* Background circle */}
        <circle
          cx="48"
          cy="48"
          r="40"
          stroke="currentColor"
          strokeWidth="8"
          fill="none"
          className="text-gray-200 dark:text-gray-700"
        />
        {/* Progress circle */}
        <circle
          cx="48"
          cy="48"
          r="40"
          stroke="currentColor"
          strokeWidth="8"
          fill="none"
          strokeLinecap="round"
          className={getScoreColor(score)}
          strokeDasharray={circumference}
          strokeDashoffset={strokeDashoffset}
          style={{ transition: 'stroke-dashoffset 0.5s ease' }}
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className={cn('text-xl font-bold sm:text-2xl', getScoreColor(score))}>
          {score}
        </span>
        <span className="text-[10px] text-gray-500 dark:text-gray-400 sm:text-xs">
          {getScoreLabel(score)}
        </span>
      </div>
    </div>
  );
}

interface StatCardProps {
  title: string;
  value: string;
  subtitle: string;
  change?: number;
  icon: React.ReactNode;
  color: 'blue' | 'purple' | 'orange' | 'green';
}

function StatCard({ title, value, subtitle, change, icon, color }: StatCardProps) {
  const colorClasses = {
    blue: 'bg-blue-500/10 text-blue-500',
    purple: 'bg-purple-500/10 text-purple-500',
    orange: 'bg-orange-500/10 text-orange-500',
    green: 'bg-green-500/10 text-green-500',
  };

  return (
    <Card className="p-3 sm:p-4">
      <div className="flex items-start justify-between">
        <div className="min-w-0 flex-1">
          <p className="truncate text-xs text-gray-500 dark:text-gray-400 sm:text-sm">{title}</p>
          <div className="mt-0.5 flex items-baseline gap-1 sm:mt-1 sm:gap-2">
            <p className="text-xl font-bold text-gray-900 dark:text-gray-100 sm:text-2xl">
              {value}
            </p>
            {change !== undefined && change !== 0 && (
              <span
                className={cn(
                  'flex items-center text-[10px] font-medium sm:text-xs',
                  change > 0 ? 'text-green-500' : 'text-red-500'
                )}
              >
                {change > 0 ? (
                  <ArrowUpRight className="h-3 w-3" />
                ) : (
                  <ArrowDownRight className="h-3 w-3" />
                )}
                {Math.abs(change)}%
              </span>
            )}
          </div>
          <p className="mt-0.5 truncate text-[10px] text-gray-500 dark:text-gray-400 sm:mt-1 sm:text-xs">{subtitle}</p>
        </div>
        <div className={cn('hidden rounded-lg p-2 sm:block', colorClasses[color])}>
          {icon}
        </div>
      </div>
    </Card>
  );
}

interface DailyTasksChartProps {
  data: DailyTaskCount[];
}

function DailyTasksChart({ data }: DailyTasksChartProps) {
  const maxCount = Math.max(...data.map((d) => d.count), 1);

  // Take last 30 days only
  const chartData = data.slice(-30);

  return (
    <div>
      <div className="flex h-24 items-end gap-px sm:h-32">
        {chartData.map((day, index) => {
          const height = day.count > 0 ? Math.max((day.count / maxCount) * 100, 4) : 0;
          return (
            <div
              key={index}
              className="group relative flex-1"
            >
              <div
                className="w-full rounded-t bg-accent/70 hover:bg-accent transition-colors"
                style={{ height: `${height}%` }}
              />
              {/* Tooltip - hidden on small touch screens, shown on hover for desktop */}
              <div className="absolute bottom-full left-1/2 mb-2 hidden -translate-x-1/2 transform whitespace-nowrap rounded bg-gray-900 px-2 py-1 text-xs text-white group-hover:block dark:bg-gray-700">
                {new Date(day.date).toLocaleDateString('fr-FR', {
                  day: 'numeric',
                  month: 'short',
                })}
                : {day.count} tache{day.count !== 1 ? 's' : ''}
              </div>
            </div>
          );
        })}
      </div>
      <div className="mt-1.5 flex justify-between text-[10px] text-gray-500 dark:text-gray-400 sm:mt-2 sm:text-xs">
        <span>
          {chartData[0] &&
            new Date(chartData[0].date).toLocaleDateString('fr-FR', {
              day: 'numeric',
              month: 'short',
            })}
        </span>
        <span>
          {chartData[chartData.length - 1] &&
            new Date(chartData[chartData.length - 1].date).toLocaleDateString('fr-FR', {
              day: 'numeric',
              month: 'short',
            })}
        </span>
      </div>
    </div>
  );
}

interface PriorityBreakdownProps {
  stats: ProductivityStats;
}

function PriorityBreakdown({ stats }: PriorityBreakdownProps) {
  const priorities = [
    {
      label: 'Urgente',
      count: stats.urgentTasksCompleted,
      color: 'bg-red-500',
      textColor: 'text-red-500',
    },
    {
      label: 'Haute',
      count: stats.highPriorityTasksCompleted,
      color: 'bg-orange-500',
      textColor: 'text-orange-500',
    },
    {
      label: 'Moyenne',
      count: stats.mediumPriorityTasksCompleted,
      color: 'bg-blue-500',
      textColor: 'text-blue-500',
    },
    {
      label: 'Basse',
      count: stats.lowPriorityTasksCompleted,
      color: 'bg-gray-400',
      textColor: 'text-gray-500',
    },
  ];

  const total = priorities.reduce((sum, p) => sum + p.count, 0);

  if (total === 0) {
    return (
      <div className="flex h-32 items-center justify-center text-sm text-gray-500 dark:text-gray-400">
        Aucune tache completee ce mois
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Stacked Bar */}
      <div className="flex h-8 overflow-hidden rounded-lg">
        {priorities.map((priority, index) => {
          const percentage = (priority.count / total) * 100;
          if (percentage === 0) return null;
          return (
            <div
              key={index}
              className={cn('transition-all', priority.color)}
              style={{ width: `${percentage}%` }}
              title={`${priority.label}: ${priority.count} (${Math.round(percentage)}%)`}
            />
          );
        })}
      </div>

      {/* Legend */}
      <div className="grid grid-cols-2 gap-2">
        {priorities.map((priority, index) => (
          <div key={index} className="flex items-center gap-2">
            <div className={cn('h-3 w-3 rounded-full', priority.color)} />
            <span className="text-xs text-gray-600 dark:text-gray-400">
              {priority.label}
            </span>
            <span className={cn('text-xs font-medium', priority.textColor)}>
              {priority.count}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

interface CompletionRateBarProps {
  label: string;
  rate: number;
  color: 'accent' | 'purple';
}

function CompletionRateBar({ label, rate, color }: CompletionRateBarProps) {
  const colorClass = color === 'accent' ? 'bg-accent' : 'bg-purple-500';

  return (
    <div>
      <div className="mb-1 flex items-center justify-between">
        <span className="text-sm text-gray-600 dark:text-gray-400">{label}</span>
        <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
          {rate}%
        </span>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
        <div
          className={cn('h-full rounded-full transition-all duration-500', colorClass)}
          style={{ width: `${Math.min(rate, 100)}%` }}
        />
      </div>
    </div>
  );
}

interface WeeklyComparisonProps {
  change: number;
}

function WeeklyComparison({ change }: WeeklyComparisonProps) {
  const isPositive = change > 0;
  const isNeutral = change === 0;

  if (isNeutral) {
    return (
      <div className="text-center">
        <div className="mb-1.5 inline-flex rounded-full bg-gray-100 p-2 dark:bg-gray-800 sm:mb-2 sm:p-3">
          <TrendingUp className="h-6 w-6 text-gray-400 sm:h-8 sm:w-8" />
        </div>
        <p className="text-xs text-gray-500 dark:text-gray-400 sm:text-sm">
          Stable par rapport a la semaine derniere
        </p>
      </div>
    );
  }

  return (
    <div className="text-center">
      <div
        className={cn(
          'mb-1.5 inline-flex rounded-full p-2 sm:mb-2 sm:p-3',
          isPositive ? 'bg-green-100 dark:bg-green-900/30' : 'bg-red-100 dark:bg-red-900/30'
        )}
      >
        {isPositive ? (
          <TrendingUp className="h-6 w-6 text-green-500 sm:h-8 sm:w-8" />
        ) : (
          <TrendingDown className="h-6 w-6 text-red-500 sm:h-8 sm:w-8" />
        )}
      </div>
      <p
        className={cn(
          'text-xl font-bold sm:text-2xl',
          isPositive ? 'text-green-500' : 'text-red-500'
        )}
      >
        {isPositive ? '+' : ''}{change}%
      </p>
      <p className="text-xs text-gray-500 dark:text-gray-400 sm:text-sm">
        vs semaine derniere
      </p>
    </div>
  );
}
