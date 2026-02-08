import { useState, useEffect, useCallback } from 'react';
import {
  Calendar,
  Clock,
  AlertTriangle,
  TrendingUp,
  Users,
  Timer,
  Activity,
  ChevronDown,
} from 'lucide-react';
import toast from 'react-hot-toast';
import Card from '../ui/Card';
import {
  ChartContainer,
  SimpleLineChart,
  SimplePieChart,
  SimpleBarChart,
  ProgressGauge,
  StatCard,
} from '../ui/Charts';
import { analyticsService } from '../../services/analytics.service';
import type { CalendarAnalytics, DayHeatmapData, AgendaConflict } from '../../types/analytics';
import { cn } from '../../lib/utils';

interface Props {
  refreshKey?: number;
}

export default function CalendarAnalyticsSection({ refreshKey }: Props) {
  const [analytics, setAnalytics] = useState<CalendarAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [dateRange, setDateRange] = useState<'7' | '30' | '90'>('30');

  const loadAnalytics = useCallback(async () => {
    try {
      setLoading(true);
      const endDate = new Date().toISOString().split('T')[0];
      const startDate = new Date(Date.now() - parseInt(dateRange) * 24 * 60 * 60 * 1000)
        .toISOString()
        .split('T')[0];
      const data = await analyticsService.getCalendarAnalytics(startDate, endDate);
      setAnalytics(data);
    } catch (error) {
      toast.error('Erreur lors du chargement des analytics calendrier');
      console.error(error);
    } finally {
      setLoading(false);
    }
  }, [dateRange]);

  useEffect(() => {
    loadAnalytics();
  }, [loadAnalytics, refreshKey]);

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-accent border-t-transparent" />
      </div>
    );
  }

  if (!analytics) {
    return (
      <div className="flex h-64 items-center justify-center">
        <p className="text-gray-500 dark:text-gray-400">Aucune donnee disponible</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Date Range Selector */}
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
          Analytics Calendrier
        </h2>
        <div className="relative">
          <select
            value={dateRange}
            onChange={(e) => setDateRange(e.target.value as '7' | '30' | '90')}
            className="appearance-none rounded-lg border border-gray-200 bg-white px-4 py-2 pr-8 text-sm text-gray-700 dark:border-gray-700 dark:bg-dark-card dark:text-gray-300"
          >
            <option value="7">7 derniers jours</option>
            <option value="30">30 derniers jours</option>
            <option value="90">90 derniers jours</option>
          </select>
          <ChevronDown className="pointer-events-none absolute right-2 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
        </div>
      </div>

      {/* Overview Stats */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Evenements"
          value={analytics.eventsInPeriod}
          subtitle={`${analytics.totalHoursScheduled}h planifiees`}
          icon={<Calendar className="h-6 w-6" />}
          color="blue"
        />
        <StatCard
          title="Duree moyenne"
          value={`${analytics.averageEventDurationHours}h`}
          subtitle="par evenement"
          icon={<Timer className="h-6 w-6" />}
          color="purple"
        />
        <StatCard
          title="Taux d'occupation"
          value={`${analytics.occupancyRate.toFixed(1)}%`}
          subtitle="heures de travail"
          icon={<Activity className="h-6 w-6" />}
          color="green"
        />
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Disponibilite</p>
              <p className="mt-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
                {analytics.availabilityScore}/100
              </p>
            </div>
            <ProgressGauge value={analytics.availabilityScore} label="" size="sm" />
          </div>
        </Card>
      </div>

      {/* Events Per Period Charts */}
      <div className="grid gap-6 lg:grid-cols-2">
        <ChartContainer title="Evenements par semaine">
          <SimpleLineChart
            data={analytics.eventsPerWeek.map((d) => ({
              date: d.label,
              value: d.eventCount,
            }))}
            color="#3B82F6"
          />
        </ChartContainer>

        <ChartContainer title="Heures planifiees par semaine">
          <SimpleBarChart
            data={analytics.eventsPerWeek.map((d) => ({
              name: d.label.replace('Semaine ', 'S'),
              value: d.totalHours,
            }))}
            color="#8B5CF6"
          />
        </ChartContainer>
      </div>

      {/* Time Distribution Pie Chart */}
      <div className="grid gap-6 lg:grid-cols-2">
        <ChartContainer title="Repartition du temps">
          <SimplePieChart
            data={Object.entries(analytics.timeDistribution).map(([name, value]) => ({
              name: name === 'Organization Events' ? 'Reunions' : 'Personnel',
              value,
            }))}
            innerRadius={40}
          />
        </ChartContainer>

        <ChartContainer title="Reunions vs Personnel">
          <div className="flex flex-col items-center justify-center h-[300px]">
            <div className="flex items-center gap-8">
              <div className="text-center">
                <p className="text-4xl font-bold text-blue-500">{analytics.meetingHours}h</p>
                <p className="text-sm text-gray-500 dark:text-gray-400">Reunions</p>
              </div>
              <div className="text-3xl text-gray-400">vs</div>
              <div className="text-center">
                <p className="text-4xl font-bold text-purple-500">{analytics.personalEventHours}h</p>
                <p className="text-sm text-gray-500 dark:text-gray-400">Personnel</p>
              </div>
            </div>
            {analytics.meetingVsPersonalRatio > 0 && (
              <p className="mt-4 text-sm text-gray-500 dark:text-gray-400">
                Ratio: {analytics.meetingVsPersonalRatio.toFixed(2)}x plus de reunions
              </p>
            )}
          </div>
        </ChartContainer>
      </div>

      {/* Occupancy Rate by Day */}
      <ChartContainer
        title="Taux d'occupation par jour"
        subtitle="Heures occupees vs disponibles"
      >
        <SimpleBarChart
          data={analytics.dailyOccupancy.slice(-14).map((d) => ({
            name: new Date(d.date).toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric' }),
            value: d.occupancyRate,
          }))}
          color="#22C55E"
        />
      </ChartContainer>

      {/* Weekly Heatmap */}
      <ChartContainer
        title="Jours les plus charges"
        subtitle="Moyenne d'heures par jour de la semaine"
      >
        <WeeklyHeatmap data={analytics.weeklyHeatmap} />
      </ChartContainer>

      {/* Time Slot Distribution */}
      <ChartContainer
        title="Distribution horaire des evenements"
        subtitle={`Creneau le plus utilise: ${analytics.mostUsedTimeSlot}`}
      >
        <TimeSlotChart data={analytics.timeSlotDistribution} />
      </ChartContainer>

      {/* Availability Insight */}
      <Card className="p-6">
        <div className="flex items-start gap-4">
          <div
            className={cn(
              'rounded-full p-3',
              analytics.availabilityScore >= 70
                ? 'bg-green-100 text-green-600 dark:bg-green-900/40 dark:text-green-400'
                : analytics.availabilityScore >= 40
                  ? 'bg-yellow-100 text-yellow-600 dark:bg-yellow-900/40 dark:text-yellow-400'
                  : 'bg-red-100 text-red-600 dark:bg-red-900/40 dark:text-red-400'
            )}
          >
            <TrendingUp className="h-6 w-6" />
          </div>
          <div>
            <h4 className="font-semibold text-gray-900 dark:text-gray-100">
              Score de disponibilite: {analytics.availabilityScore}/100
            </h4>
            <p className="mt-1 text-gray-600 dark:text-gray-400">{analytics.availabilityInsight}</p>
          </div>
        </div>
      </Card>

      {/* Conflicts Warning */}
      {analytics.conflictCount > 0 && (
        <ConflictsCard conflicts={analytics.conflicts} count={analytics.conflictCount} />
      )}

      {/* Forecast */}
      <Card className="p-6">
        <div className="flex items-start gap-4">
          <div className="rounded-full bg-blue-100 p-3 text-blue-600 dark:bg-blue-900/40 dark:text-blue-400">
            <Calendar className="h-6 w-6" />
          </div>
          <div>
            <h4 className="font-semibold text-gray-900 dark:text-gray-100">
              Prevision semaine prochaine
            </h4>
            <p className="mt-1 text-gray-600 dark:text-gray-400">
              {analytics.forecastedEventsNextWeek} evenements planifies pour{' '}
              {analytics.forecastedHoursNextWeek}h au total
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}

interface WeeklyHeatmapProps {
  data: DayHeatmapData[];
}

function WeeklyHeatmap({ data }: WeeklyHeatmapProps) {
  const dayNames: Record<string, string> = {
    MONDAY: 'Lundi',
    TUESDAY: 'Mardi',
    WEDNESDAY: 'Mercredi',
    THURSDAY: 'Jeudi',
    FRIDAY: 'Vendredi',
    SATURDAY: 'Samedi',
    SUNDAY: 'Dimanche',
  };

  const getIntensityColor = (intensity: string) => {
    switch (intensity) {
      case 'LOW':
        return 'bg-green-200 dark:bg-green-900';
      case 'MEDIUM':
        return 'bg-yellow-200 dark:bg-yellow-900';
      case 'HIGH':
        return 'bg-orange-300 dark:bg-orange-800';
      case 'VERY_HIGH':
        return 'bg-red-400 dark:bg-red-700';
      default:
        return 'bg-gray-200 dark:bg-gray-700';
    }
  };

  return (
    <div className="space-y-2">
      {data.map((day) => (
        <div key={day.dayOfWeek} className="flex items-center gap-4">
          <span className="w-24 text-sm text-gray-600 dark:text-gray-400">
            {dayNames[day.dayOfWeek]}
          </span>
          <div className="flex-1">
            <div className="flex items-center gap-2">
              <div
                className={cn(
                  'h-8 rounded transition-all',
                  getIntensityColor(day.intensity)
                )}
                style={{ width: `${Math.min(100, day.averageHours * 12.5)}%` }}
              />
              <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                {day.averageHours}h
              </span>
            </div>
          </div>
          <span
            className={cn(
              'rounded-full px-2 py-1 text-xs font-medium',
              day.intensity === 'LOW' && 'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-400',
              day.intensity === 'MEDIUM' && 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/40 dark:text-yellow-400',
              day.intensity === 'HIGH' && 'bg-orange-100 text-orange-700 dark:bg-orange-900/40 dark:text-orange-400',
              day.intensity === 'VERY_HIGH' && 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-400'
            )}
          >
            {day.averageEvents.toFixed(1)} evt/j
          </span>
        </div>
      ))}
      <div className="mt-4 flex items-center justify-end gap-4 text-xs text-gray-500 dark:text-gray-400">
        <span>Intensite:</span>
        <div className="flex items-center gap-2">
          <div className="h-3 w-3 rounded bg-green-200 dark:bg-green-900" />
          <span>Faible</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-3 w-3 rounded bg-yellow-200 dark:bg-yellow-900" />
          <span>Moyenne</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-3 w-3 rounded bg-orange-300 dark:bg-orange-800" />
          <span>Elevee</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-3 w-3 rounded bg-red-400 dark:bg-red-700" />
          <span>Tres elevee</span>
        </div>
      </div>
    </div>
  );
}

interface TimeSlotChartProps {
  data: { hour: number; timeSlot: string; eventCount: number; percentage: number }[];
}

function TimeSlotChart({ data }: TimeSlotChartProps) {
  // Filter to show only work hours (6-22)
  const workHoursData = data.filter((d) => d.hour >= 6 && d.hour <= 22);
  const maxCount = Math.max(...workHoursData.map((d) => d.eventCount), 1);

  return (
    <div className="h-48">
      <div className="flex h-full items-end gap-1">
        {workHoursData.map((slot) => (
          <div key={slot.hour} className="flex flex-1 flex-col items-center">
            <div
              className={cn(
                'w-full rounded-t transition-colors',
                slot.eventCount > 0 ? 'bg-accent hover:bg-accent/80' : 'bg-gray-200 dark:bg-gray-700'
              )}
              style={{
                height: `${slot.eventCount > 0 ? (slot.eventCount / maxCount) * 100 : 5}%`,
                minHeight: '4px',
              }}
              title={`${slot.timeSlot}: ${slot.eventCount} evenements (${slot.percentage}%)`}
            />
          </div>
        ))}
      </div>
      <div className="mt-2 flex justify-between text-xs text-gray-500 dark:text-gray-400">
        <span>6h</span>
        <span>9h</span>
        <span>12h</span>
        <span>15h</span>
        <span>18h</span>
        <span>22h</span>
      </div>
    </div>
  );
}

interface ConflictsCardProps {
  conflicts: AgendaConflict[];
  count: number;
}

function ConflictsCard({ conflicts, count }: ConflictsCardProps) {
  const [expanded, setExpanded] = useState(false);

  return (
    <Card className="border-red-200 bg-red-50 p-6 dark:border-red-900/40 dark:bg-red-900/20">
      <div className="flex items-start gap-4">
        <AlertTriangle className="h-6 w-6 shrink-0 text-red-600" />
        <div className="flex-1">
          <div className="flex items-center justify-between">
            <h4 className="font-semibold text-red-800 dark:text-red-400">
              {count} conflit{count > 1 ? 's' : ''} d'agenda detecte{count > 1 ? 's' : ''}
            </h4>
            {conflicts.length > 0 && (
              <button
                onClick={() => setExpanded(!expanded)}
                className="text-sm text-red-600 hover:text-red-700 dark:text-red-400"
              >
                {expanded ? 'Masquer' : 'Voir les details'}
              </button>
            )}
          </div>
          <p className="mt-1 text-sm text-red-700 dark:text-red-300">
            Vous avez des evenements qui se chevauchent. Pensez a reorganiser votre agenda.
          </p>
          {expanded && conflicts.length > 0 && (
            <div className="mt-4 space-y-3">
              {conflicts.slice(0, 5).map((conflict, index) => (
                <div
                  key={index}
                  className="rounded-lg bg-white p-3 dark:bg-gray-800"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Users className="h-4 w-4 text-red-500" />
                      <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                        {conflict.event1Title}
                      </span>
                    </div>
                    <span className="text-xs text-gray-500 dark:text-gray-400">
                      {conflict.conflictDate}
                    </span>
                  </div>
                  <div className="mt-1 flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                    <Clock className="h-3 w-3" />
                    <span>Chevauche avec "{conflict.event2Title}"</span>
                  </div>
                  <div className="mt-1 text-xs text-red-600 dark:text-red-400">
                    {conflict.overlapMinutes} min de chevauchement ({conflict.conflictTime})
                  </div>
                </div>
              ))}
              {conflicts.length > 5 && (
                <p className="text-sm text-red-600 dark:text-red-400">
                  ... et {conflicts.length - 5} autre{conflicts.length - 5 > 1 ? 's' : ''} conflit{conflicts.length - 5 > 1 ? 's' : ''}
                </p>
              )}
            </div>
          )}
        </div>
      </div>
    </Card>
  );
}
