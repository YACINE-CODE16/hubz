import { useState, useEffect } from 'react';
import {
  Target,
  TrendingUp,
  AlertTriangle,
  CheckCircle,
  Clock,
  BarChart3,
  ArrowRight
} from 'lucide-react';
import toast from 'react-hot-toast';
import Card from '../ui/Card';
import { goalService } from '../../services/goal.service';
import type { GoalAnalytics, GoalProgress } from '../../types/goal';

interface Props {
  organizationId?: string;
  refreshKey?: number;
}

export default function GoalAnalyticsComponent({ organizationId, refreshKey }: Props) {
  const [analytics, setAnalytics] = useState<GoalAnalytics | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadAnalytics();
  }, [organizationId, refreshKey]);

  const loadAnalytics = async () => {
    try {
      setLoading(true);
      const data = organizationId
        ? await goalService.getOrganizationAnalytics(organizationId)
        : await goalService.getPersonalAnalytics();
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

  if (!analytics || analytics.totalGoals === 0) {
    return null;
  }

  return (
    <div className="space-y-6">
      <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
        Analyse des objectifs
      </h2>

      {/* Summary Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Progression globale"
          value={`${analytics.overallProgressPercentage}%`}
          subtitle={`${analytics.completedGoals}/${analytics.totalGoals} termines`}
          icon={<Target className="h-5 w-5" />}
          color="blue"
        />
        <StatCard
          title="En bonne voie"
          value={analytics.goalsOnTrack.toString()}
          subtitle="objectifs sur la bonne trajectoire"
          icon={<CheckCircle className="h-5 w-5" />}
          color="green"
        />
        <StatCard
          title="A risque"
          value={analytics.atRiskGoals.toString()}
          subtitle="objectifs necessitant attention"
          icon={<AlertTriangle className="h-5 w-5" />}
          color={analytics.atRiskGoals > 0 ? 'red' : 'gray'}
        />
        <StatCard
          title="Velocite moyenne"
          value={analytics.averageVelocity.toFixed(2)}
          subtitle="taches/jour"
          icon={<TrendingUp className="h-5 w-5" />}
          color="purple"
        />
      </div>

      {/* Goals at Risk */}
      {analytics.goalsAtRisk.length > 0 && (
        <Card className="p-6 border-l-4 border-l-red-500">
          <h3 className="mb-4 text-lg font-semibold text-red-600 dark:text-red-400 flex items-center gap-2">
            <AlertTriangle className="h-5 w-5" />
            Objectifs a risque
          </h3>
          <div className="space-y-3">
            {analytics.goalsAtRisk.map((goal) => (
              <RiskGoalCard key={goal.goalId} goal={goal} />
            ))}
          </div>
        </Card>
      )}

      {/* Progress by Type */}
      <Card className="p-6">
        <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
          <BarChart3 className="mr-2 inline h-5 w-5" />
          Progression par type
        </h3>
        <div className="grid gap-4 sm:grid-cols-3">
          {(['SHORT', 'MEDIUM', 'LONG'] as const).map((type) => {
            const count = analytics.goalsByType[type] || 0;
            const avgProgress = analytics.avgProgressByType[type] || 0;
            return (
              <TypeProgressCard
                key={type}
                type={type}
                count={count}
                avgProgress={avgProgress}
              />
            );
          })}
        </div>
      </Card>

      {/* Individual Goal Progress */}
      {analytics.goalProgressList.length > 0 && (
        <Card className="p-6">
          <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
            Progression detaillee
          </h3>
          <div className="space-y-4">
            {analytics.goalProgressList.map((goal) => (
              <GoalProgressRow key={goal.goalId} goal={goal} />
            ))}
          </div>
        </Card>
      )}

      {/* Completion History */}
      {analytics.completionHistory.length > 0 && (
        <Card className="p-6">
          <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
            <Clock className="mr-2 inline h-5 w-5" />
            Historique des completions
          </h3>
          <CompletionHistoryChart data={analytics.completionHistory} />
        </Card>
      )}
    </div>
  );
}

interface StatCardProps {
  title: string;
  value: string;
  subtitle: string;
  icon: React.ReactNode;
  color: 'blue' | 'green' | 'red' | 'purple' | 'gray';
}

function StatCard({ title, value, subtitle, icon, color }: StatCardProps) {
  const colorClasses = {
    blue: 'bg-blue-500/10 text-blue-500',
    green: 'bg-green-500/10 text-green-500',
    red: 'bg-red-500/10 text-red-500',
    purple: 'bg-purple-500/10 text-purple-500',
    gray: 'bg-gray-500/10 text-gray-500',
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

interface RiskGoalCardProps {
  goal: GoalProgress;
}

function RiskGoalCard({ goal }: RiskGoalCardProps) {
  return (
    <div className="flex items-center justify-between rounded-lg bg-red-50 dark:bg-red-900/20 p-4">
      <div className="flex-1">
        <div className="flex items-center gap-2">
          <p className="font-medium text-gray-900 dark:text-gray-100">
            {goal.title}
          </p>
          <span className="text-xs px-2 py-0.5 rounded bg-red-100 dark:bg-red-900 text-red-600 dark:text-red-400">
            {goal.daysRemaining < 0 ? 'En retard' : `${goal.daysRemaining}j restants`}
          </span>
        </div>
        <p className="text-sm text-red-600 dark:text-red-400 mt-1">
          {goal.riskReason}
        </p>
      </div>
      <div className="text-right ml-4">
        <p className="text-lg font-bold text-gray-900 dark:text-gray-100">
          {goal.progressPercentage}%
        </p>
        <p className="text-xs text-gray-500 dark:text-gray-400">
          {goal.completedTasks}/{goal.totalTasks} taches
        </p>
      </div>
    </div>
  );
}

interface TypeProgressCardProps {
  type: 'SHORT' | 'MEDIUM' | 'LONG';
  count: number;
  avgProgress: number;
}

function TypeProgressCard({ type, count, avgProgress }: TypeProgressCardProps) {
  const typeLabels = {
    SHORT: 'Court terme',
    MEDIUM: 'Moyen terme',
    LONG: 'Long terme',
  };

  const typeColors = {
    SHORT: 'bg-orange-500',
    MEDIUM: 'bg-blue-500',
    LONG: 'bg-purple-500',
  };

  return (
    <div className="rounded-lg bg-gray-50 dark:bg-dark-hover p-4">
      <div className="flex items-center justify-between mb-2">
        <span className="font-medium text-gray-900 dark:text-gray-100">
          {typeLabels[type]}
        </span>
        <span className="text-sm text-gray-500 dark:text-gray-400">
          {count} objectif{count > 1 ? 's' : ''}
        </span>
      </div>
      <div className="h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
        <div
          className={`h-full ${typeColors[type]} transition-all duration-300`}
          style={{ width: `${Math.min(avgProgress, 100)}%` }}
        />
      </div>
      <p className="text-right text-sm text-gray-500 dark:text-gray-400 mt-1">
        {avgProgress.toFixed(1)}% en moyenne
      </p>
    </div>
  );
}

interface GoalProgressRowProps {
  goal: GoalProgress;
}

function GoalProgressRow({ goal }: GoalProgressRowProps) {
  const getStatusColor = () => {
    if (goal.progressPercentage >= 100) return 'text-green-500';
    if (goal.isAtRisk) return 'text-red-500';
    if (goal.isOnTrack) return 'text-blue-500';
    return 'text-yellow-500';
  };

  const getStatusIcon = () => {
    if (goal.progressPercentage >= 100) return <CheckCircle className="h-4 w-4" />;
    if (goal.isAtRisk) return <AlertTriangle className="h-4 w-4" />;
    return <Clock className="h-4 w-4" />;
  };

  return (
    <div className="rounded-lg bg-gray-50 dark:bg-dark-hover p-4">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <span className={getStatusColor()}>{getStatusIcon()}</span>
          <span className="font-medium text-gray-900 dark:text-gray-100">
            {goal.title}
          </span>
          <span className="text-xs px-2 py-0.5 rounded bg-gray-200 dark:bg-gray-700 text-gray-600 dark:text-gray-400">
            {goal.type === 'SHORT' ? 'Court terme' : goal.type === 'MEDIUM' ? 'Moyen terme' : 'Long terme'}
          </span>
        </div>
        <div className="flex items-center gap-4 text-sm text-gray-500 dark:text-gray-400">
          {goal.deadline && (
            <span>{goal.daysRemaining >= 0 ? `${goal.daysRemaining}j` : 'En retard'}</span>
          )}
          <span className="font-medium text-gray-900 dark:text-gray-100">
            {goal.progressPercentage}%
          </span>
        </div>
      </div>

      {/* Progress bar */}
      <div className="h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden mb-2">
        <div
          className={`h-full transition-all duration-300 ${
            goal.progressPercentage >= 100 ? 'bg-green-500' :
            goal.isAtRisk ? 'bg-red-500' :
            goal.isOnTrack ? 'bg-blue-500' : 'bg-yellow-500'
          }`}
          style={{ width: `${Math.min(goal.progressPercentage, 100)}%` }}
        />
      </div>

      {/* Details row */}
      <div className="flex items-center justify-between text-xs text-gray-500 dark:text-gray-400">
        <span>{goal.completedTasks}/{goal.totalTasks} taches</span>
        <div className="flex items-center gap-4">
          <span>Velocite: {goal.velocityPerDay} t/j</span>
          {goal.predictedCompletionDate && goal.predictedCompletionDate !== 'Completed' && (
            <span className="flex items-center gap-1">
              <ArrowRight className="h-3 w-3" />
              Fin prevue: {goal.predictedCompletionDate}
            </span>
          )}
        </div>
      </div>
    </div>
  );
}

interface CompletionHistoryChartProps {
  data: { month: string; completedCount: number }[];
}

function CompletionHistoryChart({ data }: CompletionHistoryChartProps) {
  const maxCount = Math.max(...data.map(d => d.completedCount), 1);

  return (
    <div className="h-32">
      <div className="flex h-full items-end justify-around gap-2">
        {data.map((item, index) => (
          <div key={index} className="flex flex-col items-center gap-2 flex-1">
            <div
              className="w-full max-w-12 bg-accent/80 hover:bg-accent rounded-t transition-colors"
              style={{
                height: `${(item.completedCount / maxCount) * 100}%`,
                minHeight: item.completedCount > 0 ? '8px' : '0'
              }}
              title={`${item.month}: ${item.completedCount} objectif(s)`}
            />
            <span className="text-xs text-gray-500 dark:text-gray-400">
              {item.month.slice(5)}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
