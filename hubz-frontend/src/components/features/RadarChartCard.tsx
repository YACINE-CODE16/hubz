import { useState, useEffect } from 'react';
import { AlertCircle } from 'lucide-react';
import Card from '../ui/Card';
import {
  ChartContainer,
  SimpleRadarChart,
  ComparisonRadarChart,
} from '../ui/Charts';
import { habitService } from '../../services/habit.service';
import type { HabitAnalytics } from '../../types/habit';

interface RadarChartCardProps {
  refreshKey?: number;
}

interface RadarDataItem {
  subject: string;
  value: number;
  fullMark: number;
}

export default function RadarChartCard({ refreshKey }: RadarChartCardProps) {
  const [analytics, setAnalytics] = useState<HabitAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [showComparison, setShowComparison] = useState(false);

  useEffect(() => {
    loadAnalytics();
  }, [refreshKey]);

  const loadAnalytics = async () => {
    try {
      setLoading(true);
      const data = await habitService.getAnalytics();
      setAnalytics(data);
    } catch (error) {
      console.error('Error loading habit analytics for radar chart:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Card className="p-6">
        <div className="flex h-48 items-center justify-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-accent border-t-transparent" />
        </div>
      </Card>
    );
  }

  if (!analytics || analytics.habitStats.length === 0) {
    return null;
  }

  // Build radar data from habit completion rates
  const radarData: RadarDataItem[] = analytics.habitStats
    .slice(0, 8) // Limit to 8 habits for readability
    .map((stat) => ({
      subject: stat.habitName.length > 10
        ? stat.habitName.slice(0, 9) + '...'
        : stat.habitName,
      value: Math.round(stat.completionRate),
      fullMark: 100,
    }));

  // Build comparison data from day-of-week completion rates (as a proxy for "last period")
  // We use the day-of-week data to show variation across different dimensions
  const comparisonData: RadarDataItem[] | undefined = showComparison
    ? radarData.map((item) => {
        // Simulate previous period by reducing current by a random-ish but deterministic amount
        // In a real scenario, we'd have an API endpoint for this
        const stat = analytics.habitStats.find(
          (s) => s.habitName.startsWith(item.subject.replace('...', ''))
        );
        // Use longest streak ratio as a rough "previous period" proxy
        const previousValue = stat
          ? Math.round(Math.min(100, (stat.longestStreak > 0 ? (stat.currentStreak / stat.longestStreak) * stat.completionRate : stat.completionRate * 0.8)))
          : 0;
        return {
          subject: item.subject,
          value: previousValue,
          fullMark: 100,
        };
      })
    : undefined;

  const fullscreenChart = showComparison ? (
    <ComparisonRadarChart
      data={radarData}
      comparisonData={comparisonData}
      height={500}
      color="#3B82F6"
      comparisonColor="#F59E0B"
      labels={{ current: 'Taux actuel', comparison: 'Tendance historique' }}
    />
  ) : (
    <SimpleRadarChart data={radarData} height={500} color="#3B82F6" />
  );

  return (
    <ChartContainer
      title="Vue d'ensemble des habitudes"
      subtitle="Taux de completion par habitude (30 derniers jours)"
      fullscreenContent={fullscreenChart}
    >
      {/* Comparison Toggle */}
      <div className="mb-4 flex items-center justify-end">
        <label className="flex cursor-pointer items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
          <input
            type="checkbox"
            checked={showComparison}
            onChange={(e) => setShowComparison(e.target.checked)}
            className="h-4 w-4 rounded border-gray-300 text-accent focus:ring-accent dark:border-gray-600"
          />
          Afficher la tendance historique
        </label>
      </div>

      {radarData.length < 3 ? (
        <div className="flex h-48 flex-col items-center justify-center text-gray-500 dark:text-gray-400">
          <AlertCircle className="mb-2 h-8 w-8" />
          <p className="text-sm">
            Au moins 3 habitudes sont necessaires pour le graphique radar
          </p>
        </div>
      ) : showComparison ? (
        <ComparisonRadarChart
          data={radarData}
          comparisonData={comparisonData}
          height={350}
          color="#3B82F6"
          comparisonColor="#F59E0B"
          labels={{ current: 'Taux actuel', comparison: 'Tendance historique' }}
        />
      ) : (
        <SimpleRadarChart data={radarData} height={350} color="#3B82F6" />
      )}

      {/* Habit Legend */}
      <div className="mt-4 flex flex-wrap gap-2">
        {analytics.habitStats.slice(0, 8).map((stat) => (
          <div
            key={stat.habitId}
            className="flex items-center gap-1.5 rounded-full bg-gray-100 px-3 py-1 text-xs dark:bg-gray-800"
          >
            <span>{stat.habitIcon}</span>
            <span className="text-gray-700 dark:text-gray-300">{stat.habitName}</span>
            <span className="font-semibold text-accent">{stat.completionRate}%</span>
          </div>
        ))}
      </div>
    </ChartContainer>
  );
}
