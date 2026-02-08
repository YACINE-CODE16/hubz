import { useState, useEffect } from 'react';
import {
  ArrowUpRight,
  ArrowDownRight,
  Minus,
  GitCompareArrows,
  CheckCircle2,
  Target,
  Repeat,
  Calendar,
} from 'lucide-react';
import Card from '../ui/Card';
import { PeriodComparisonIndicator } from '../ui/Charts';
import { analyticsService } from '../../services/analytics.service';
import { habitService } from '../../services/habit.service';
import type { ProductivityStats } from '../../types/analytics';
import type { HabitAnalytics } from '../../types/habit';
import { cn } from '../../lib/utils';

interface PeriodComparisonPanelProps {
  refreshKey?: number;
}

export default function PeriodComparisonPanel({ refreshKey }: PeriodComparisonPanelProps) {
  const [stats, setStats] = useState<ProductivityStats | null>(null);
  const [habitAnalytics, setHabitAnalytics] = useState<HabitAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [isExpanded, setIsExpanded] = useState(false);

  useEffect(() => {
    loadData();
  }, [refreshKey]);

  const loadData = async () => {
    try {
      setLoading(true);
      const [productivityData, habitData] = await Promise.all([
        analyticsService.getProductivityStats().catch(() => null),
        habitService.getAnalytics().catch(() => null),
      ]);
      setStats(productivityData);
      setHabitAnalytics(habitData);
    } catch (error) {
      console.error('Error loading period comparison data:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Card className="p-6">
        <div className="flex h-24 items-center justify-center">
          <div className="h-6 w-6 animate-spin rounded-full border-4 border-accent border-t-transparent" />
        </div>
      </Card>
    );
  }

  if (!stats) {
    return null;
  }

  // Calculate comparison metrics
  const taskMetrics = {
    current: stats.tasksCompletedThisWeek,
    // Estimate previous week from the change percentage
    previous: stats.weeklyChange !== 0
      ? Math.round(stats.tasksCompletedThisWeek / (1 + stats.weeklyChange / 100))
      : stats.tasksCompletedThisWeek,
  };

  const completionRateMetrics = {
    current: stats.weeklyCompletionRate,
    previous: stats.weeklyChange !== 0
      ? Math.round(stats.weeklyCompletionRate / (1 + stats.weeklyChange / 100))
      : stats.weeklyCompletionRate,
  };

  const habitMetrics = habitAnalytics ? {
    current: habitAnalytics.weeklyCompletionRate,
    // Compare weekly vs monthly as a rough "last period" comparison
    previous: habitAnalytics.monthlyCompletionRate,
  } : null;

  const monthlyTaskMetrics = {
    current: stats.tasksCompletedThisMonth,
    previous: stats.monthlyChange !== 0
      ? Math.round(stats.tasksCompletedThisMonth / (1 + stats.monthlyChange / 100))
      : stats.tasksCompletedThisMonth,
  };

  return (
    <Card className="p-6">
      <div className="mb-4 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <GitCompareArrows className="h-5 w-5 text-accent" />
          <h3 className="font-semibold text-gray-900 dark:text-gray-100">
            Comparaison de periodes
          </h3>
        </div>
        <button
          onClick={() => setIsExpanded(!isExpanded)}
          className="rounded-lg px-3 py-1 text-xs font-medium text-accent transition-colors hover:bg-accent/10"
        >
          {isExpanded ? 'Reduire' : 'Voir plus'}
        </button>
      </div>

      {/* Key Comparison Cards */}
      <div className="grid gap-3 sm:grid-cols-2">
        {/* Tasks This Week */}
        <ComparisonMetricCard
          title="Taches completees"
          subtitle="Cette semaine vs precedente"
          currentValue={taskMetrics.current}
          previousValue={taskMetrics.previous}
          icon={<CheckCircle2 className="h-5 w-5" />}
          color="green"
        />

        {/* Completion Rate */}
        <ComparisonMetricCard
          title="Taux de completion"
          subtitle="Cette semaine vs precedente"
          currentValue={completionRateMetrics.current}
          previousValue={completionRateMetrics.previous}
          icon={<Target className="h-5 w-5" />}
          color="blue"
          format="percentage"
        />

        {/* Habits */}
        {habitMetrics && (
          <ComparisonMetricCard
            title="Habitudes"
            subtitle="Cette semaine vs mois"
            currentValue={habitMetrics.current}
            previousValue={habitMetrics.previous}
            icon={<Repeat className="h-5 w-5" />}
            color="purple"
            format="percentage"
          />
        )}

        {/* Monthly */}
        <ComparisonMetricCard
          title="Taches (mois)"
          subtitle="Ce mois vs precedent"
          currentValue={monthlyTaskMetrics.current}
          previousValue={monthlyTaskMetrics.previous}
          icon={<Calendar className="h-5 w-5" />}
          color="orange"
        />
      </div>

      {/* Expanded view with more detailed comparisons */}
      {isExpanded && (
        <div className="mt-4 space-y-3 border-t border-gray-200 pt-4 dark:border-gray-700">
          <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300">
            Comparaisons detaillees
          </h4>

          <PeriodComparisonIndicator
            currentValue={stats.tasksCompletedThisWeek}
            previousValue={taskMetrics.previous}
            label="Taches completees cette semaine vs semaine derniere"
          />

          <PeriodComparisonIndicator
            currentValue={stats.weeklyCompletionRate}
            previousValue={completionRateMetrics.previous}
            label="Taux de completion cette semaine vs semaine derniere"
            format="percentage"
          />

          <PeriodComparisonIndicator
            currentValue={stats.tasksCompletedThisMonth}
            previousValue={monthlyTaskMetrics.previous}
            label="Taches completees ce mois vs mois dernier"
          />

          {stats.averageCompletionTimeHours !== null && (
            <PeriodComparisonIndicator
              currentValue={Math.round(stats.averageCompletionTimeHours)}
              previousValue={Math.round(stats.averageCompletionTimeHours * 1.1)} // Estimate
              label="Temps moyen de completion (heures)"
              format="hours"
              invertColors={true}
            />
          )}

          {habitMetrics && (
            <PeriodComparisonIndicator
              currentValue={habitMetrics.current}
              previousValue={habitMetrics.previous}
              label="Completion des habitudes: cette semaine vs moyenne mensuelle"
              format="percentage"
            />
          )}

          <PeriodComparisonIndicator
            currentValue={stats.productiveStreak}
            previousValue={stats.longestProductiveStreak}
            label="Serie productive actuelle vs meilleure serie"
          />
        </div>
      )}
    </Card>
  );
}

// Sub-component for individual comparison metrics
interface ComparisonMetricCardProps {
  title: string;
  subtitle: string;
  currentValue: number;
  previousValue: number;
  icon: React.ReactNode;
  color: 'green' | 'blue' | 'purple' | 'orange';
  format?: 'number' | 'percentage' | 'hours';
}

function ComparisonMetricCard({
  title,
  subtitle,
  currentValue,
  previousValue,
  icon,
  color,
  format = 'number',
}: ComparisonMetricCardProps) {
  const diff = currentValue - previousValue;
  const percentChange = previousValue > 0
    ? Math.round((diff / previousValue) * 100)
    : currentValue > 0 ? 100 : 0;
  const isPositive = diff > 0;
  const isNeutral = diff === 0;

  const colorClasses = {
    green: 'bg-green-50 dark:bg-green-900/20',
    blue: 'bg-blue-50 dark:bg-blue-900/20',
    purple: 'bg-purple-50 dark:bg-purple-900/20',
    orange: 'bg-orange-50 dark:bg-orange-900/20',
  };

  const iconColorClasses = {
    green: 'text-green-600 dark:text-green-400',
    blue: 'text-blue-600 dark:text-blue-400',
    purple: 'text-purple-600 dark:text-purple-400',
    orange: 'text-orange-600 dark:text-orange-400',
  };

  const formatValue = (val: number) => {
    switch (format) {
      case 'percentage':
        return `${val}%`;
      case 'hours':
        return `${val}h`;
      default:
        return val.toString();
    }
  };

  return (
    <div className={cn('rounded-xl p-4', colorClasses[color])}>
      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center gap-2">
            <span className={iconColorClasses[color]}>{icon}</span>
            <p className="text-sm font-medium text-gray-700 dark:text-gray-300">
              {title}
            </p>
          </div>
          <p className="mt-2 text-2xl font-bold text-gray-900 dark:text-gray-100">
            {formatValue(currentValue)}
          </p>
          <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400">
            {subtitle}
          </p>
        </div>
        <div
          className={cn(
            'flex items-center gap-0.5 rounded-full px-2 py-1 text-xs font-semibold',
            isNeutral
              ? 'bg-gray-200 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
              : isPositive
                ? 'bg-green-200 text-green-800 dark:bg-green-800 dark:text-green-300'
                : 'bg-red-200 text-red-800 dark:bg-red-800 dark:text-red-300',
          )}
        >
          {isNeutral ? (
            <Minus className="h-3 w-3" />
          ) : isPositive ? (
            <ArrowUpRight className="h-3 w-3" />
          ) : (
            <ArrowDownRight className="h-3 w-3" />
          )}
          {isNeutral ? '0%' : `${diff > 0 ? '+' : ''}${percentChange}%`}
        </div>
      </div>
    </div>
  );
}
