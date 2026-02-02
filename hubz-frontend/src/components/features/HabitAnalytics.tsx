import { useState, useEffect } from 'react';
import {
  TrendingUp,
  Target,
  Calendar,
  Award,
  Flame,
  BarChart3
} from 'lucide-react';
import toast from 'react-hot-toast';
import Card from '../ui/Card';
import { habitService } from '../../services/habit.service';
import type { HabitAnalytics, HeatmapData, TrendData, HabitStats } from '../../types/habit';

interface Props {
  refreshKey?: number;
}

export default function HabitAnalyticsComponent({ refreshKey }: Props) {
  const [analytics, setAnalytics] = useState<HabitAnalytics | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadAnalytics();
  }, [refreshKey]);

  const loadAnalytics = async () => {
    try {
      setLoading(true);
      const data = await habitService.getAnalytics();
      setAnalytics(data);
    } catch (error) {
      toast.error('Erreur lors du chargement des analytics');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-accent border-t-transparent" />
      </div>
    );
  }

  if (!analytics || analytics.totalHabits === 0) {
    return null;
  }

  return (
    <div className="space-y-6">
      <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
        Statistiques
      </h2>

      {/* Summary Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Taux de completion"
          value={`${analytics.dailyCompletionRate}%`}
          subtitle="Aujourd'hui"
          icon={<Target className="h-5 w-5" />}
          color="blue"
        />
        <StatCard
          title="Serie actuelle"
          value={analytics.currentStreak.toString()}
          subtitle="jours consecutifs"
          icon={<Flame className="h-5 w-5" />}
          color="orange"
        />
        <StatCard
          title="Meilleure serie"
          value={analytics.longestStreak.toString()}
          subtitle={analytics.bestStreakHabitName || 'jours'}
          icon={<Award className="h-5 w-5" />}
          color="yellow"
        />
        <StatCard
          title="Ce mois"
          value={`${analytics.monthlyCompletionRate}%`}
          subtitle="taux de completion"
          icon={<BarChart3 className="h-5 w-5" />}
          color="green"
        />
      </div>

      {/* Heatmap Calendar */}
      <Card className="p-6">
        <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
          <Calendar className="mr-2 inline h-5 w-5" />
          Calendrier de completion
        </h3>
        <HeatmapCalendar data={analytics.completionHeatmap} />
      </Card>

      {/* Individual Habit Stats */}
      {analytics.habitStats.length > 0 && (
        <Card className="p-6">
          <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
            <TrendingUp className="mr-2 inline h-5 w-5" />
            Performance par habitude
          </h3>
          <div className="space-y-4">
            {analytics.habitStats.map((stat) => (
              <HabitStatRow key={stat.habitId} stat={stat} />
            ))}
          </div>
        </Card>
      )}

      {/* Weekly Trend */}
      <Card className="p-6">
        <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
          Tendance des 30 derniers jours
        </h3>
        <TrendChart data={analytics.last30DaysTrend} />
      </Card>

      {/* Day of Week Analysis */}
      <Card className="p-6">
        <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
          Performance par jour de la semaine
        </h3>
        <DayOfWeekChart data={analytics.completionByDayOfWeek} />
      </Card>
    </div>
  );
}

interface StatCardProps {
  title: string;
  value: string;
  subtitle: string;
  icon: React.ReactNode;
  color: 'blue' | 'orange' | 'yellow' | 'green';
}

function StatCard({ title, value, subtitle, icon, color }: StatCardProps) {
  const colorClasses = {
    blue: 'bg-blue-500/10 text-blue-500',
    orange: 'bg-orange-500/10 text-orange-500',
    yellow: 'bg-yellow-500/10 text-yellow-500',
    green: 'bg-green-500/10 text-green-500',
  };

  return (
    <Card className="p-4">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm text-gray-500 dark:text-gray-400">{title}</p>
          <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-gray-100">
            {value}
          </p>
          <p className="text-xs text-gray-500 dark:text-gray-400">{subtitle}</p>
        </div>
        <div className={`rounded-lg p-2 ${colorClasses[color]}`}>
          {icon}
        </div>
      </div>
    </Card>
  );
}

interface HeatmapCalendarProps {
  data: HeatmapData[];
}

function HeatmapCalendar({ data }: HeatmapCalendarProps) {
  // Get last 12 weeks (84 days) for a more compact view
  const recentData = data.slice(-84);

  // Group by week
  const weeks: HeatmapData[][] = [];
  for (let i = 0; i < recentData.length; i += 7) {
    weeks.push(recentData.slice(i, i + 7));
  }

  const getColorClass = (rate: number) => {
    if (rate === 0) return 'bg-gray-200 dark:bg-gray-700';
    if (rate < 25) return 'bg-green-200 dark:bg-green-900';
    if (rate < 50) return 'bg-green-300 dark:bg-green-700';
    if (rate < 75) return 'bg-green-400 dark:bg-green-600';
    return 'bg-green-500 dark:bg-green-500';
  };

  return (
    <div className="overflow-x-auto">
      <div className="flex gap-1">
        {weeks.map((week, weekIndex) => (
          <div key={weekIndex} className="flex flex-col gap-1">
            {week.map((day, dayIndex) => (
              <div
                key={dayIndex}
                className={`h-3 w-3 rounded-sm ${getColorClass(day.completionRate)}`}
                title={`${day.date}: ${day.completedCount}/${day.totalHabits} (${day.completionRate}%)`}
              />
            ))}
          </div>
        ))}
      </div>
      <div className="mt-2 flex items-center justify-end gap-2 text-xs text-gray-500 dark:text-gray-400">
        <span>Moins</span>
        <div className="flex gap-1">
          <div className="h-3 w-3 rounded-sm bg-gray-200 dark:bg-gray-700" />
          <div className="h-3 w-3 rounded-sm bg-green-200 dark:bg-green-900" />
          <div className="h-3 w-3 rounded-sm bg-green-300 dark:bg-green-700" />
          <div className="h-3 w-3 rounded-sm bg-green-400 dark:bg-green-600" />
          <div className="h-3 w-3 rounded-sm bg-green-500" />
        </div>
        <span>Plus</span>
      </div>
    </div>
  );
}

interface HabitStatRowProps {
  stat: HabitStats;
}

function HabitStatRow({ stat }: HabitStatRowProps) {
  return (
    <div className="flex items-center justify-between rounded-lg bg-gray-50 dark:bg-dark-hover p-4">
      <div className="flex items-center gap-3">
        <span className="text-2xl">{stat.habitIcon}</span>
        <div>
          <p className="font-medium text-gray-900 dark:text-gray-100">
            {stat.habitName}
          </p>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {stat.frequency === 'DAILY' ? 'Quotidien' : 'Hebdomadaire'}
          </p>
        </div>
      </div>
      <div className="flex items-center gap-6">
        <div className="text-center">
          <p className="text-lg font-bold text-gray-900 dark:text-gray-100">
            {stat.currentStreak}
          </p>
          <p className="text-xs text-gray-500 dark:text-gray-400">Serie</p>
        </div>
        <div className="text-center">
          <p className="text-lg font-bold text-gray-900 dark:text-gray-100">
            {stat.longestStreak}
          </p>
          <p className="text-xs text-gray-500 dark:text-gray-400">Record</p>
        </div>
        <div className="text-center">
          <p className="text-lg font-bold text-gray-900 dark:text-gray-100">
            {stat.completionRate}%
          </p>
          <p className="text-xs text-gray-500 dark:text-gray-400">30 jours</p>
        </div>
        <div className="text-center">
          <p className="text-lg font-bold text-gray-900 dark:text-gray-100">
            {stat.totalCompletions}
          </p>
          <p className="text-xs text-gray-500 dark:text-gray-400">Total</p>
        </div>
      </div>
    </div>
  );
}

interface TrendChartProps {
  data: TrendData[];
}

function TrendChart({ data }: TrendChartProps) {
  const maxRate = Math.max(...data.map(d => d.completionRate), 100);

  return (
    <div className="h-32">
      <div className="flex h-full items-end gap-px">
        {data.map((day, index) => (
          <div
            key={index}
            className="flex-1 bg-accent/80 hover:bg-accent rounded-t transition-colors"
            style={{ height: `${(day.completionRate / maxRate) * 100}%`, minHeight: day.completionRate > 0 ? '4px' : '0' }}
            title={`${day.date}: ${day.completionRate}%`}
          />
        ))}
      </div>
      <div className="mt-2 flex justify-between text-xs text-gray-500 dark:text-gray-400">
        <span>{data[0]?.date}</span>
        <span>{data[data.length - 1]?.date}</span>
      </div>
    </div>
  );
}

interface DayOfWeekChartProps {
  data: Record<string, number>;
}

function DayOfWeekChart({ data }: DayOfWeekChartProps) {
  const dayNames: Record<string, string> = {
    MONDAY: 'Lun',
    TUESDAY: 'Mar',
    WEDNESDAY: 'Mer',
    THURSDAY: 'Jeu',
    FRIDAY: 'Ven',
    SATURDAY: 'Sam',
    SUNDAY: 'Dim',
  };

  const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
  const maxRate = Math.max(...Object.values(data), 100);

  return (
    <div className="flex items-end justify-around h-32 gap-2">
      {days.map((day) => {
        const rate = data[day] || 0;
        return (
          <div key={day} className="flex flex-col items-center gap-2">
            <div
              className="w-8 bg-accent/80 hover:bg-accent rounded-t transition-colors"
              style={{ height: `${(rate / maxRate) * 100}%`, minHeight: rate > 0 ? '4px' : '0' }}
              title={`${dayNames[day]}: ${rate}%`}
            />
            <span className="text-xs text-gray-500 dark:text-gray-400">
              {dayNames[day]}
            </span>
          </div>
        );
      })}
    </div>
  );
}
