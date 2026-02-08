import { Target, Edit2, Trash2, CheckCircle2, AlertTriangle, Clock } from 'lucide-react';
import Card from '../ui/Card';
import type { Goal, GoalType } from '../../types/goal';

interface GoalCardProps {
  goal: Goal;
  onEdit: (goal: Goal) => void;
  onDelete: (id: string) => void;
}

/**
 * Calculate the deadline status for a goal.
 * Returns the number of days until the deadline and the urgency level.
 */
function getDeadlineStatus(deadline: string | undefined): {
  daysUntil: number | null;
  urgency: 'overdue' | 'critical' | 'warning' | 'approaching' | 'normal' | null;
} {
  if (!deadline) {
    return { daysUntil: null, urgency: null };
  }

  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const deadlineDate = new Date(deadline);
  deadlineDate.setHours(0, 0, 0, 0);

  const diffTime = deadlineDate.getTime() - today.getTime();
  const daysUntil = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

  let urgency: 'overdue' | 'critical' | 'warning' | 'approaching' | 'normal';
  if (daysUntil < 0) {
    urgency = 'overdue';
  } else if (daysUntil <= 1) {
    urgency = 'critical';
  } else if (daysUntil <= 3) {
    urgency = 'warning';
  } else if (daysUntil <= 7) {
    urgency = 'approaching';
  } else {
    urgency = 'normal';
  }

  return { daysUntil, urgency };
}

/**
 * Deadline badge component that displays the urgency of a goal's deadline.
 */
function DeadlineBadge({ deadline, isCompleted }: { deadline: string | undefined; isCompleted: boolean }) {
  if (!deadline || isCompleted) {
    return null;
  }

  const { daysUntil, urgency } = getDeadlineStatus(deadline);

  if (urgency === 'normal' || daysUntil === null) {
    return null;
  }

  const badgeConfig = {
    overdue: {
      bg: 'bg-red-500/10 dark:bg-red-500/20',
      text: 'text-red-600 dark:text-red-400',
      border: 'border-red-500/30',
      icon: AlertTriangle,
      label: 'En retard',
    },
    critical: {
      bg: 'bg-red-500/10 dark:bg-red-500/20',
      text: 'text-red-600 dark:text-red-400',
      border: 'border-red-500/30',
      icon: AlertTriangle,
      label: daysUntil === 0 ? "Aujourd'hui" : 'Demain',
    },
    warning: {
      bg: 'bg-orange-500/10 dark:bg-orange-500/20',
      text: 'text-orange-600 dark:text-orange-400',
      border: 'border-orange-500/30',
      icon: Clock,
      label: `${daysUntil} jours`,
    },
    approaching: {
      bg: 'bg-yellow-500/10 dark:bg-yellow-500/20',
      text: 'text-yellow-600 dark:text-yellow-400',
      border: 'border-yellow-500/30',
      icon: Clock,
      label: `${daysUntil} jours`,
    },
    normal: {
      bg: '',
      text: '',
      border: '',
      icon: Clock,
      label: '',
    },
  };

  const config = badgeConfig[urgency];
  const Icon = config.icon;

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-lg px-2 py-0.5 text-xs font-medium border ${config.bg} ${config.text} ${config.border}`}
      title={`Echeance: ${new Date(deadline).toLocaleDateString('fr-FR')}`}
    >
      <Icon className="h-3 w-3" />
      {config.label}
    </span>
  );
}

export default function GoalCard({ goal, onEdit, onDelete }: GoalCardProps) {
  const progress = goal.totalTasks > 0 ? (goal.completedTasks / goal.totalTasks) * 100 : 0;
  const isCompleted = goal.totalTasks > 0 && goal.completedTasks === goal.totalTasks;
  const { urgency } = getDeadlineStatus(goal.deadline);

  const typeColors: Record<GoalType, string> = {
    SHORT: 'bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20',
    MEDIUM: 'bg-purple-500/10 text-purple-600 dark:text-purple-400 border border-purple-500/20',
    LONG: 'bg-orange-500/10 text-orange-600 dark:text-orange-400 border border-orange-500/20',
  };

  const typeLabels: Record<GoalType, string> = {
    SHORT: 'Court terme',
    MEDIUM: 'Moyen terme',
    LONG: 'Long terme',
  };

  // Determine card border color based on urgency
  const urgencyBorderClass = !isCompleted && urgency ? {
    overdue: 'ring-2 ring-red-500/50',
    critical: 'ring-2 ring-red-500/50',
    warning: 'ring-2 ring-orange-500/50',
    approaching: 'ring-1 ring-yellow-500/30',
    normal: '',
  }[urgency] : '';

  return (
    <Card className={`group relative flex flex-col gap-4 p-4 transition-all hover:shadow-lg ${urgencyBorderClass}`}>
      {/* Header */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            {isCompleted ? (
              <CheckCircle2 className="h-5 w-5 flex-shrink-0 text-green-500" />
            ) : (
              <Target className="h-5 w-5 flex-shrink-0 text-gray-400" />
            )}
            <h3 className="font-semibold text-gray-900 dark:text-gray-100 truncate">
              {goal.title}
            </h3>
          </div>
          <div className="mt-2 flex flex-wrap items-center gap-2">
            <span
              className={`inline-block rounded-lg px-2.5 py-1 text-xs font-medium ${typeColors[goal.type]}`}
            >
              {typeLabels[goal.type]}
            </span>
            <DeadlineBadge deadline={goal.deadline} isCompleted={isCompleted} />
          </div>
        </div>
        <div className="flex gap-1 opacity-100 sm:opacity-0 sm:group-hover:opacity-100 transition-opacity">
          <button
            onClick={() => onEdit(goal)}
            className="rounded-lg p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-800 dark:hover:text-gray-300 sm:p-1.5"
            title="Modifier"
          >
            <Edit2 className="h-4 w-4" />
          </button>
          <button
            onClick={() => onDelete(goal.id)}
            className="rounded-lg p-2 text-gray-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/20 dark:hover:text-red-400 sm:p-1.5"
            title="Supprimer"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Description */}
      {goal.description && (
        <p className="text-sm text-gray-600 dark:text-gray-400 line-clamp-2">
          {goal.description}
        </p>
      )}

      {/* Progress Bar */}
      <div className="space-y-2">
        <div className="flex items-center justify-between text-sm">
          <span className="text-gray-600 dark:text-gray-400">Progression</span>
          <span className="font-medium text-gray-900 dark:text-gray-100">
            {goal.completedTasks} / {goal.totalTasks} taches
            {isCompleted && ' '}
          </span>
        </div>
        <div className="h-2 overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
          <div
            className={`h-full rounded-full transition-all ${
              isCompleted
                ? 'bg-green-500'
                : progress >= 75
                  ? 'bg-blue-500'
                  : progress >= 50
                    ? 'bg-purple-500'
                    : 'bg-orange-500'
            }`}
            style={{ width: `${Math.min(progress, 100)}%` }}
          />
        </div>
        {goal.totalTasks === 0 && (
          <p className="text-xs text-gray-500 dark:text-gray-400 italic">
            Aucune tache associee. Creez des taches et liez-les a cet objectif.
          </p>
        )}
      </div>

      {/* Footer */}
      {goal.deadline && (
        <div className="flex items-center justify-between pt-2 border-t border-gray-200 dark:border-gray-700">
          <span className="text-xs text-gray-500 dark:text-gray-400">
            Echeance: {new Date(goal.deadline).toLocaleDateString('fr-FR')}
          </span>
        </div>
      )}
    </Card>
  );
}

// Export the utility function for use in other components
export { getDeadlineStatus };
