import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Target,
  Calendar,
  TrendingUp,
  CheckCircle2,
  Clock,
  Flame,
  BarChart3,
  ArrowRight
} from 'lucide-react';
import toast from 'react-hot-toast';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import { goalService } from '../../services/goal.service';
import { eventService } from '../../services/event.service';
import { habitService } from '../../services/habit.service';
import type { Goal } from '../../types/goal';
import type { Event } from '../../types/event';
import type { Habit, HabitLog } from '../../types/habit';

export default function PersonalRecapPage() {
  const navigate = useNavigate();
  const [goals, setGoals] = useState<Goal[]>([]);
  const [events, setEvents] = useState<Event[]>([]);
  const [habits, setHabits] = useState<Habit[]>([]);
  const [habitLogs, setHabitLogs] = useState<Record<string, HabitLog[]>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);

      // Charger toutes les données en parallèle
      const [goalsData, eventsData, habitsData] = await Promise.all([
        goalService.getPersonalGoals(),
        eventService.getPersonalEvents(),
        habitService.getUserHabits(),
      ]);

      setGoals(goalsData);
      setEvents(eventsData);
      setHabits(habitsData);

      // Charger les logs d'habitudes
      const logsPromises = habitsData.map(async (habit) => {
        const logs = await habitService.getHabitLogs(habit.id);
        return { habitId: habit.id, logs };
      });

      const logsResults = await Promise.all(logsPromises);
      const logsMap: Record<string, HabitLog[]> = {};
      logsResults.forEach(({ habitId, logs }) => {
        logsMap[habitId] = logs;
      });
      setHabitLogs(logsMap);
    } catch (error) {
      toast.error('Erreur lors du chargement des données');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  // Calculs statistiques
  const completedGoals = goals.filter(
    (g) => g.totalTasks > 0 && g.completedTasks === g.totalTasks
  ).length;

  const inProgressGoals = goals.filter(
    (g) => g.totalTasks > 0 && g.completedTasks < g.totalTasks
  ).length;

  const totalTasks = goals.reduce((sum, g) => sum + g.totalTasks, 0);
  const completedTasks = goals.reduce((sum, g) => sum + g.completedTasks, 0);
  const tasksProgress = totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0;

  // Événements à venir (7 prochains jours)
  const today = new Date();
  const in7Days = new Date();
  in7Days.setDate(today.getDate() + 7);

  const upcomingEvents = events
    .filter((e) => {
      const eventDate = new Date(e.startTime);
      return eventDate >= today && eventDate <= in7Days;
    })
    .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
    .slice(0, 5);

  // Statistiques habitudes
  const todayStr = today.toISOString().split('T')[0];
  const completedToday = habits.filter((habit) => {
    const logs = habitLogs[habit.id] || [];
    const todayLog = logs.find((log) => log.date === todayStr);
    return todayLog?.completed;
  }).length;

  // Calcul du streak (jours consécutifs)
  const calculateStreak = () => {
    if (habits.length === 0) return 0;

    let streak = 0;
    let currentDate = new Date();

    while (streak < 30) { // Max 30 jours pour éviter boucle infinie
      const dateStr = currentDate.toISOString().split('T')[0];
      let allCompleted = true;

      for (const habit of habits) {
        const logs = habitLogs[habit.id] || [];
        const log = logs.find((l) => l.date === dateStr);
        if (!log || !log.completed) {
          allCompleted = false;
          break;
        }
      }

      if (!allCompleted) break;
      streak++;
      currentDate.setDate(currentDate.getDate() - 1);
    }

    return streak;
  };

  const streak = calculateStreak();

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">Chargement...</div>
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col gap-6 overflow-auto p-6">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
          Récapitulatif
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
                {goals.length}
              </p>
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                {completedGoals} complétés
              </p>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30">
              <Target className="h-6 w-6 text-blue-600 dark:text-blue-400" />
            </div>
          </div>
        </Card>

        {/* Progression tâches */}
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">Tâches</p>
              <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-gray-100">
                {tasksProgress}%
              </p>
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                {completedTasks}/{totalTasks} complétées
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
                {completedToday}/{habits.length}
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
              <p className="text-sm text-gray-500 dark:text-gray-400">Série</p>
              <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-gray-100">
                {streak}
              </p>
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                jours consécutifs
              </p>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-orange-100 dark:bg-orange-900/30">
              <Flame className="h-6 w-6 text-orange-600 dark:text-orange-400" />
            </div>
          </div>
        </Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Objectifs en cours */}
        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Target className="h-5 w-5 text-gray-400" />
              <h3 className="font-semibold text-gray-900 dark:text-gray-100">
                Objectifs en cours
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
                Aucun objectif créé
              </p>
              <Button
                onClick={() => navigate('/personal/goals')}
                className="mt-4"
              >
                Créer un objectif
              </Button>
            </div>
          ) : (
            <div className="space-y-3">
              {goals.slice(0, 3).map((goal) => {
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
                        className={`h-full rounded-full transition-all ${
                          isCompleted ? 'bg-green-500' : 'bg-accent'
                        }`}
                        style={{ width: `${Math.min(progress, 100)}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </Card>

        {/* Événements à venir */}
        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Calendar className="h-5 w-5 text-gray-400" />
              <h3 className="font-semibold text-gray-900 dark:text-gray-100">
                Événements à venir
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
                Aucun événement prévu dans les 7 prochains jours
              </p>
              <Button
                onClick={() => navigate('/personal/calendar')}
                className="mt-4"
              >
                Créer un événement
              </Button>
            </div>
          ) : (
            <div className="space-y-3">
              {upcomingEvents.map((event) => {
                const startDate = new Date(event.startTime);
                const isToday =
                  startDate.toDateString() === today.toDateString();

                return (
                  <div
                    key={event.id}
                    className={`rounded-lg border p-3 transition-all hover:border-accent ${
                      isToday
                        ? 'border-accent bg-accent/5'
                        : 'border-gray-200 dark:border-gray-700'
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      <div className="flex h-10 w-10 flex-shrink-0 flex-col items-center justify-center rounded-lg bg-gray-100 dark:bg-gray-800">
                        <span className="text-xs font-medium text-gray-600 dark:text-gray-400">
                          {startDate.toLocaleDateString('fr-FR', {
                            month: 'short',
                          })}
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
      </div>

      {/* Graphique habitudes */}
      <Card className="p-6">
        <div className="mb-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <BarChart3 className="h-5 w-5 text-gray-400" />
            <h3 className="font-semibold text-gray-900 dark:text-gray-100">
              Progression des habitudes (7 derniers jours)
            </h3>
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

        {habits.length === 0 ? (
          <div className="py-8 text-center">
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Aucune habitude créée
            </p>
            <Button
              onClick={() => navigate('/personal/habits')}
              className="mt-4"
            >
              Créer une habitude
            </Button>
          </div>
        ) : (
          <div className="space-y-4">
            {habits.slice(0, 5).map((habit) => {
              const logs = habitLogs[habit.id] || [];
              const last7Days = [];

              for (let i = 6; i >= 0; i--) {
                const date = new Date();
                date.setDate(date.getDate() - i);
                const dateStr = date.toISOString().split('T')[0];
                const log = logs.find((l) => l.date === dateStr);
                last7Days.push({
                  date: dateStr,
                  completed: log?.completed || false,
                });
              }

              const completedCount = last7Days.filter((d) => d.completed).length;

              return (
                <div key={habit.id} className="space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="text-lg">{habit.icon}</span>
                      <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                        {habit.name}
                      </span>
                    </div>
                    <span className="text-sm text-gray-600 dark:text-gray-400">
                      {completedCount}/7
                    </span>
                  </div>
                  <div className="flex gap-1">
                    {last7Days.map((day, index) => (
                      <div
                        key={index}
                        className={`h-8 flex-1 rounded ${
                          day.completed
                            ? 'bg-green-500'
                            : 'bg-gray-200 dark:bg-gray-700'
                        }`}
                        title={new Date(day.date).toLocaleDateString('fr-FR')}
                      />
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </Card>
    </div>
  );
}
