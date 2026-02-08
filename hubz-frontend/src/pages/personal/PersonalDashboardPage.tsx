import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Target,
  Calendar,
  TrendingUp,
  CheckCircle2,
  Clock,
  Flame,
  ArrowRight,
  AlertTriangle,
  CheckSquare,
  Repeat,
  Circle,
  BarChart3,
  Check,
} from 'lucide-react';
import toast from 'react-hot-toast';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import ProductivityStatsSection from '../../components/features/ProductivityStatsSection';
import InsightsPanel from '../../components/features/InsightsPanel';
import CalendarAnalyticsSection from '../../components/features/CalendarAnalyticsSection';
import ContributionHeatmap from '../../components/features/ContributionHeatmap';
import RadarChartCard from '../../components/features/RadarChartCard';
import PeriodComparisonPanel from '../../components/features/PeriodComparisonPanel';
import { dashboardService } from '../../services/dashboard.service';
import { habitService } from '../../services/habit.service';
import { taskService } from '../../services/task.service';
import { analyticsService } from '../../services/analytics.service';
import type { PersonalDashboard, HabitWithStatus } from '../../types/dashboard';
import type { Task, TaskPriority, TaskStatus } from '../../types/task';
import type { HabitAnalytics } from '../../types/habit';
import type { ActivityHeatmap } from '../../types/analytics';
import { cn } from '../../lib/utils';

const priorityConfig: Record<TaskPriority, { label: string; className: string }> = {
  LOW: { label: 'Basse', className: 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400' },
  MEDIUM: { label: 'Moyenne', className: 'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-400' },
  HIGH: { label: 'Haute', className: 'bg-orange-100 text-orange-700 dark:bg-orange-900/40 dark:text-orange-400' },
  URGENT: { label: 'Urgente', className: 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-400' },
};


export default function PersonalDashboardPage() {
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState<PersonalDashboard | null>(null);
  const [habitAnalytics, setHabitAnalytics] = useState<HabitAnalytics | null>(null);
  const [activityHeatmap, setActivityHeatmap] = useState<ActivityHeatmap | null>(null);
  const [heatmapLoading, setHeatmapLoading] = useState(true);
  const [loading, setLoading] = useState(true);
  const [togglingHabit, setTogglingHabit] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [dashboardData, analyticsData] = await Promise.all([
        dashboardService.getPersonalDashboard(),
        habitService.getAnalytics().catch(() => null),
      ]);
      setDashboard(dashboardData);
      setHabitAnalytics(analyticsData);
    } catch (error) {
      toast.error('Erreur lors du chargement du tableau de bord');
      console.error(error);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadActivityHeatmap = useCallback(async () => {
    try {
      setHeatmapLoading(true);
      const heatmapData = await analyticsService.getUserActivityHeatmap();
      setActivityHeatmap(heatmapData);
    } catch (error) {
      console.error('Error loading activity heatmap:', error);
      // Don't show toast error for heatmap - it's a non-critical feature
    } finally {
      setHeatmapLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
    loadActivityHeatmap();
  }, [loadData, loadActivityHeatmap]);

  const handleToggleHabit = async (habitWithStatus: HabitWithStatus) => {
    const today = new Date().toISOString().split('T')[0];
    setTogglingHabit(habitWithStatus.habit.id);
    try {
      await habitService.logHabit(habitWithStatus.habit.id, {
        date: today,
        completed: !habitWithStatus.completedToday,
      });
      await loadData();
      toast.success(
        habitWithStatus.completedToday
          ? 'Habitude marquee comme non completee'
          : 'Habitude completee !'
      );
    } catch (error) {
      toast.error('Erreur lors de la mise a jour de l\'habitude');
      console.error(error);
    } finally {
      setTogglingHabit(null);
    }
  };

  const handleTaskStatusChange = async (task: Task, newStatus: TaskStatus) => {
    try {
      await taskService.updateStatus(task.id, { status: newStatus });
      await loadData();
      toast.success(
        newStatus === 'DONE' ? 'Tache terminee !' : 'Statut mis a jour'
      );
    } catch (error) {
      toast.error('Erreur lors de la mise a jour de la tache');
      console.error(error);
    }
  };

  const today = new Date();

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">Chargement...</div>
      </div>
    );
  }

  if (!dashboard) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">
          Impossible de charger les donnees
        </div>
      </div>
    );
  }

  const { stats, todayTasks, todayHabits, upcomingEvents, goals } = dashboard;

  const tasksProgress = stats.totalTasks > 0
    ? Math.round((stats.completedTasks / stats.totalTasks) * 100)
    : 0;

  return (
    <div className="flex h-full flex-col gap-6 overflow-auto p-6">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
          Mon tableau de bord
        </h2>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Vue d'ensemble de votre espace personnel
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Objectifs */}
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">Objectifs</p>
              <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-gray-100">
                {stats.totalGoals}
              </p>
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                {stats.completedGoals} completes
              </p>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30">
              <Target className="h-6 w-6 text-blue-600 dark:text-blue-400" />
            </div>
          </div>
        </Card>

        {/* Progression taches */}
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">Taches</p>
              <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-gray-100">
                {tasksProgress}%
              </p>
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                {stats.completedTasks}/{stats.totalTasks} completees
                {stats.overdueTasks > 0 && (
                  <span className="ml-1 text-red-500">
                    ({stats.overdueTasks} en retard)
                  </span>
                )}
              </p>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-purple-100 dark:bg-purple-900/30">
              <CheckCircle2 className="h-6 w-6 text-purple-600 dark:text-purple-400" />
            </div>
          </div>
        </Card>

        {/* Habitudes aujourd'hui */}
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">Habitudes</p>
              <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-gray-100">
                {stats.completedHabitsToday}/{stats.totalHabits}
              </p>
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                aujourd'hui
              </p>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-green-100 dark:bg-green-900/30">
              <TrendingUp className="h-6 w-6 text-green-600 dark:text-green-400" />
            </div>
          </div>
        </Card>

        {/* Streak */}
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">Serie</p>
              <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-gray-100">
                {stats.currentStreak}
              </p>
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                jours consecutifs
              </p>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-orange-100 dark:bg-orange-900/30">
              <Flame className="h-6 w-6 text-orange-600 dark:text-orange-400" />
            </div>
          </div>
        </Card>
      </div>

      {/* Insights Panel - Recommendations and Insights */}
      <InsightsPanel />

      {/* Period Comparison - This week vs Last week */}
      <PeriodComparisonPanel />

      {/* Contribution Heatmap - GitHub style activity calendar */}
      <ContributionHeatmap
        data={activityHeatmap}
        loading={heatmapLoading}
        title="Mon activite"
        showStats={true}
      />

      {/* Main Content Grid */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Mes taches du jour */}
        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <CheckSquare className="h-5 w-5 text-gray-400" />
              <h3 className="font-semibold text-gray-900 dark:text-gray-100">
                Mes taches du jour
              </h3>
              {todayTasks.length > 0 && (
                <span className="rounded-full bg-accent/10 px-2 py-0.5 text-xs font-medium text-accent">
                  {todayTasks.length}
                </span>
              )}
            </div>
          </div>

          {todayTasks.length === 0 ? (
            <div className="py-8 text-center">
              <CheckCircle2 className="mx-auto h-12 w-12 text-green-500" />
              <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
                Aucune tache pour aujourd'hui !
              </p>
              <p className="text-xs text-gray-400 dark:text-gray-500">
                Vous etes a jour
              </p>
            </div>
          ) : (
            <div className="space-y-2 max-h-64 overflow-y-auto">
              {todayTasks.map((task) => {
                const isOverdue = task.dueDate && new Date(task.dueDate) < today;
                const priority = task.priority ? priorityConfig[task.priority] : null;

                return (
                  <div
                    key={task.id}
                    className={cn(
                      'flex items-center gap-3 rounded-lg border p-3 transition-all hover:border-accent',
                      isOverdue ? 'border-red-300 dark:border-red-800' : 'border-gray-200 dark:border-gray-700'
                    )}
                  >
                    <button
                      onClick={() => handleTaskStatusChange(task, 'DONE')}
                      className="flex-shrink-0 text-gray-400 hover:text-green-500 transition-colors"
                      title="Marquer comme termine"
                    >
                      <Circle className="h-5 w-5" />
                    </button>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                        {task.title}
                      </p>
                      <div className="mt-1 flex flex-wrap items-center gap-2">
                        {priority && (
                          <span className={cn('rounded-full px-2 py-0.5 text-xs font-medium', priority.className)}>
                            {priority.label}
                          </span>
                        )}
                        {task.dueDate && (
                          <span className={cn(
                            'inline-flex items-center gap-1 text-xs',
                            isOverdue ? 'text-red-500 font-medium' : 'text-gray-500 dark:text-gray-400'
                          )}>
                            {isOverdue && <AlertTriangle className="h-3 w-3" />}
                            <Calendar className="h-3 w-3" />
                            {new Date(task.dueDate).toLocaleDateString('fr-FR', {
                              day: 'numeric',
                              month: 'short',
                            })}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </Card>

        {/* Mes habitudes du jour - Enhanced Visual Tracker */}
        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Repeat className="h-5 w-5 text-gray-400" />
              <h3 className="font-semibold text-gray-900 dark:text-gray-100">
                Mes habitudes du jour
              </h3>
              {todayHabits.length > 0 && (
                <span className="rounded-full bg-accent/10 px-2 py-0.5 text-xs font-medium text-accent">
                  {todayHabits.filter(h => h.completedToday).length}/{todayHabits.length}
                </span>
              )}
            </div>
            <Button
              variant="secondary"
              onClick={() => navigate('/personal/habits')}
              className="flex items-center gap-1 text-sm"
            >
              Voir tout
              <ArrowRight className="h-4 w-4" />
            </Button>
          </div>

          {todayHabits.length === 0 ? (
            <div className="py-8 text-center">
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Aucune habitude creee
              </p>
              <Button
                onClick={() => navigate('/personal/habits')}
                className="mt-4"
              >
                Creer une habitude
              </Button>
            </div>
          ) : (
            <>
              {/* Visual Progress Bar */}
              <div className="mb-4">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs text-gray-500 dark:text-gray-400">
                    Progression aujourd'hui
                  </span>
                  <span className="text-xs font-medium text-gray-700 dark:text-gray-300">
                    {Math.round((todayHabits.filter(h => h.completedToday).length / todayHabits.length) * 100)}%
                  </span>
                </div>
                <div className="h-2 overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
                  <div
                    className="h-full rounded-full bg-gradient-to-r from-green-400 to-green-600 transition-all duration-500"
                    style={{
                      width: `${(todayHabits.filter(h => h.completedToday).length / todayHabits.length) * 100}%`,
                    }}
                  />
                </div>
              </div>

              {/* Habits List with Visual Tracker */}
              <div className="space-y-3 max-h-72 overflow-y-auto">
                {todayHabits.map((habitWithStatus) => {
                  const isToggling = togglingHabit === habitWithStatus.habit.id;
                  const weekProgress = habitWithStatus.completedLast7Days;

                  return (
                    <div
                      key={habitWithStatus.habit.id}
                      className={cn(
                        'rounded-lg border p-3 transition-all',
                        habitWithStatus.completedToday
                          ? 'border-green-300 bg-green-50 dark:border-green-800 dark:bg-green-900/20'
                          : 'border-gray-200 hover:border-accent dark:border-gray-700',
                        isToggling && 'opacity-50 pointer-events-none'
                      )}
                    >
                      <div className="flex items-center gap-3">
                        {/* Toggle Button */}
                        <button
                          onClick={() => handleToggleHabit(habitWithStatus)}
                          disabled={isToggling}
                          className={cn(
                            'flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full transition-all duration-300',
                            habitWithStatus.completedToday
                              ? 'bg-green-500 text-white shadow-lg shadow-green-500/30 scale-110'
                              : 'bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700'
                          )}
                        >
                          {isToggling ? (
                            <div className="h-5 w-5 animate-spin rounded-full border-2 border-current border-t-transparent" />
                          ) : habitWithStatus.completedToday ? (
                            <Check className="h-5 w-5" />
                          ) : (
                            <span className="text-xl">{habitWithStatus.habit.icon}</span>
                          )}
                        </button>

                        {/* Habit Info */}
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between">
                            <p className={cn(
                              'text-sm font-medium truncate',
                              habitWithStatus.completedToday
                                ? 'text-green-700 dark:text-green-400'
                                : 'text-gray-900 dark:text-gray-100'
                            )}>
                              {habitWithStatus.habit.name}
                            </p>
                            {habitWithStatus.currentStreak > 0 && (
                              <span className="flex items-center gap-1 rounded-full bg-orange-100 dark:bg-orange-900/30 px-2 py-0.5 text-xs font-medium text-orange-600 dark:text-orange-400">
                                <Flame className="h-3 w-3" />
                                {habitWithStatus.currentStreak}
                              </span>
                            )}
                          </div>

                          {/* Mini Week Progress Bar */}
                          <div className="mt-2 flex items-center gap-1">
                            {[...Array(7)].map((_, i) => (
                              <div
                                key={i}
                                className={cn(
                                  'h-1.5 flex-1 rounded-full transition-all',
                                  i < weekProgress
                                    ? 'bg-green-500'
                                    : 'bg-gray-200 dark:bg-gray-700'
                                )}
                              />
                            ))}
                            <span className="ml-2 text-xs text-gray-500 dark:text-gray-400">
                              {weekProgress}/7
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Quick Stats Footer */}
              {habitAnalytics && (
                <div className="mt-4 grid grid-cols-3 gap-2 rounded-lg bg-gray-50 dark:bg-gray-800/50 p-3">
                  <div className="text-center">
                    <p className="text-lg font-bold text-gray-900 dark:text-gray-100">
                      {habitAnalytics.currentStreak}
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">Serie actuelle</p>
                  </div>
                  <div className="text-center border-x border-gray-200 dark:border-gray-700">
                    <p className="text-lg font-bold text-gray-900 dark:text-gray-100">
                      {habitAnalytics.longestStreak}
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">Meilleure serie</p>
                  </div>
                  <div className="text-center">
                    <p className="text-lg font-bold text-gray-900 dark:text-gray-100">
                      {habitAnalytics.weeklyCompletionRate}%
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">Cette semaine</p>
                  </div>
                </div>
              )}
            </>
          )}
        </Card>

        {/* Mes prochains evenements */}
        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Calendar className="h-5 w-5 text-gray-400" />
              <h3 className="font-semibold text-gray-900 dark:text-gray-100">
                Mes prochains evenements
              </h3>
            </div>
            <Button
              variant="secondary"
              onClick={() => navigate('/personal/calendar')}
              className="flex items-center gap-1 text-sm"
            >
              Voir tout
              <ArrowRight className="h-4 w-4" />
            </Button>
          </div>

          {upcomingEvents.length === 0 ? (
            <div className="py-8 text-center">
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Aucun evenement prevu dans les 7 prochains jours
              </p>
              <Button
                onClick={() => navigate('/personal/calendar')}
                className="mt-4"
              >
                Creer un evenement
              </Button>
            </div>
          ) : (
            <div className="space-y-3 max-h-64 overflow-y-auto">
              {upcomingEvents.map((event) => {
                const startDate = new Date(event.startTime);
                const isToday = startDate.toDateString() === today.toDateString();

                return (
                  <div
                    key={event.id}
                    className={cn(
                      'rounded-lg border p-3 transition-all hover:border-accent',
                      isToday
                        ? 'border-accent bg-accent/5'
                        : 'border-gray-200 dark:border-gray-700'
                    )}
                  >
                    <div className="flex items-start gap-3">
                      <div className="flex h-10 w-10 flex-shrink-0 flex-col items-center justify-center rounded-lg bg-gray-100 dark:bg-gray-800">
                        <span className="text-xs font-medium text-gray-600 dark:text-gray-400">
                          {startDate.toLocaleDateString('fr-FR', { month: 'short' })}
                        </span>
                        <span className="text-sm font-bold text-gray-900 dark:text-gray-100">
                          {startDate.getDate()}
                        </span>
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-gray-900 dark:text-gray-100 truncate">
                          {event.title}
                        </p>
                        <div className="mt-1 flex items-center gap-1 text-xs text-gray-500 dark:text-gray-400">
                          <Clock className="h-3 w-3" />
                          {startDate.toLocaleTimeString('fr-FR', {
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                          {isToday && (
                            <span className="ml-2 rounded-full bg-accent/10 px-2 py-0.5 text-xs font-medium text-accent">
                              Aujourd'hui
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </Card>

        {/* Progression de mes objectifs */}
        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Target className="h-5 w-5 text-gray-400" />
              <h3 className="font-semibold text-gray-900 dark:text-gray-100">
                Progression de mes objectifs
              </h3>
            </div>
            <Button
              variant="secondary"
              onClick={() => navigate('/personal/goals')}
              className="flex items-center gap-1 text-sm"
            >
              Voir tout
              <ArrowRight className="h-4 w-4" />
            </Button>
          </div>

          {goals.length === 0 ? (
            <div className="py-8 text-center">
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Aucun objectif cree
              </p>
              <Button
                onClick={() => navigate('/personal/goals')}
                className="mt-4"
              >
                Creer un objectif
              </Button>
            </div>
          ) : (
            <div className="space-y-3 max-h-64 overflow-y-auto">
              {goals.slice(0, 5).map((goal) => {
                const progress =
                  goal.totalTasks > 0
                    ? (goal.completedTasks / goal.totalTasks) * 100
                    : 0;
                const isCompleted =
                  goal.totalTasks > 0 && goal.completedTasks === goal.totalTasks;

                return (
                  <div
                    key={goal.id}
                    className="rounded-lg border border-gray-200 p-3 transition-all hover:border-accent dark:border-gray-700"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        {isCompleted ? (
                          <CheckCircle2 className="h-4 w-4 text-green-500" />
                        ) : (
                          <Target className="h-4 w-4 text-gray-400" />
                        )}
                        <span className="font-medium text-gray-900 dark:text-gray-100">
                          {goal.title}
                        </span>
                      </div>
                      <span className="text-sm text-gray-600 dark:text-gray-400">
                        {Math.round(progress)}%
                      </span>
                    </div>
                    <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
                      <div
                        className={cn(
                          'h-full rounded-full transition-all',
                          isCompleted ? 'bg-green-500' : 'bg-accent'
                        )}
                        style={{ width: `${Math.min(progress, 100)}%` }}
                      />
                    </div>
                    {goal.deadline && (
                      <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                        Echeance: {new Date(goal.deadline).toLocaleDateString('fr-FR')}
                      </p>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </Card>
      </div>

      {/* Habit Completion Heatmap - Visual Tracking */}
      {habitAnalytics && habitAnalytics.completionHeatmap && habitAnalytics.completionHeatmap.length > 0 && (
        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <BarChart3 className="h-5 w-5 text-gray-400" />
              <h3 className="font-semibold text-gray-900 dark:text-gray-100">
                Calendrier de completion des habitudes
              </h3>
            </div>
            <Button
              variant="secondary"
              onClick={() => navigate('/personal/habits')}
              className="flex items-center gap-1 text-sm"
            >
              Statistiques detaillees
              <ArrowRight className="h-4 w-4" />
            </Button>
          </div>

          <MiniHeatmapCalendar data={habitAnalytics.completionHeatmap} />
        </Card>
      )}

      {/* Radar Chart - Habits Overview */}
      <RadarChartCard />

      {/* Productivity Statistics Section */}
      <ProductivityStatsSection />

      {/* Calendar Analytics Section */}
      <CalendarAnalyticsSection />
    </div>
  );
}

// Mini Heatmap Calendar Component
interface HeatmapData {
  date: string;
  completedCount: number;
  totalHabits: number;
  completionRate: number;
}

interface MiniHeatmapCalendarProps {
  data: HeatmapData[];
}

function MiniHeatmapCalendar({ data }: MiniHeatmapCalendarProps) {
  // Get last 8 weeks (56 days) for a compact view
  const recentData = data.slice(-56);

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

  const dayLabels = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];

  return (
    <div className="overflow-x-auto">
      <div className="flex gap-1">
        {/* Day labels */}
        <div className="flex flex-col gap-1 mr-1">
          {dayLabels.map((day, index) => (
            <div
              key={index}
              className="h-4 w-4 flex items-center justify-center text-[10px] text-gray-400"
            >
              {index % 2 === 0 ? day : ''}
            </div>
          ))}
        </div>

        {/* Weeks */}
        {weeks.map((week, weekIndex) => (
          <div key={weekIndex} className="flex flex-col gap-1">
            {week.map((day, dayIndex) => (
              <div
                key={dayIndex}
                className={cn(
                  'h-4 w-4 rounded-sm transition-all cursor-pointer hover:ring-2 hover:ring-accent hover:ring-offset-1',
                  getColorClass(day.completionRate)
                )}
                title={`${new Date(day.date).toLocaleDateString('fr-FR', {
                  weekday: 'short',
                  day: 'numeric',
                  month: 'short',
                })}: ${day.completedCount}/${day.totalHabits} (${Math.round(day.completionRate)}%)`}
              />
            ))}
          </div>
        ))}
      </div>

      {/* Legend */}
      <div className="mt-3 flex items-center justify-between">
        <span className="text-xs text-gray-500 dark:text-gray-400">
          8 dernieres semaines
        </span>
        <div className="flex items-center gap-1 text-xs text-gray-500 dark:text-gray-400">
          <span>Moins</span>
          <div className="flex gap-0.5">
            <div className="h-3 w-3 rounded-sm bg-gray-200 dark:bg-gray-700" />
            <div className="h-3 w-3 rounded-sm bg-green-200 dark:bg-green-900" />
            <div className="h-3 w-3 rounded-sm bg-green-300 dark:bg-green-700" />
            <div className="h-3 w-3 rounded-sm bg-green-400 dark:bg-green-600" />
            <div className="h-3 w-3 rounded-sm bg-green-500" />
          </div>
          <span>Plus</span>
        </div>
      </div>
    </div>
  );
}
