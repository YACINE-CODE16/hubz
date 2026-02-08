import { useState, useEffect } from 'react';
import { History, ChevronDown, User, Calendar, AlertTriangle, Target, Type, FileText, UserCheck, Filter } from 'lucide-react';
import type { TaskHistory, TaskHistoryField } from '../../types/task';
import { taskHistoryService } from '../../services/taskHistory.service';
import { cn } from '../../lib/utils';

interface TaskHistoryTimelineProps {
  taskId: string;
}

const fieldLabels: Record<TaskHistoryField, string> = {
  TITLE: 'Titre',
  DESCRIPTION: 'Description',
  STATUS: 'Statut',
  PRIORITY: 'Priorite',
  ASSIGNEE: 'Assignation',
  DUE_DATE: 'Date limite',
  GOAL: 'Objectif',
};

const fieldIcons: Record<TaskHistoryField, React.ReactNode> = {
  TITLE: <Type className="h-4 w-4" />,
  DESCRIPTION: <FileText className="h-4 w-4" />,
  STATUS: <History className="h-4 w-4" />,
  PRIORITY: <AlertTriangle className="h-4 w-4" />,
  ASSIGNEE: <UserCheck className="h-4 w-4" />,
  DUE_DATE: <Calendar className="h-4 w-4" />,
  GOAL: <Target className="h-4 w-4" />,
};

const statusLabels: Record<string, string> = {
  TODO: 'A faire',
  IN_PROGRESS: 'En cours',
  DONE: 'Termine',
};

const priorityLabels: Record<string, string> = {
  LOW: 'Basse',
  MEDIUM: 'Moyenne',
  HIGH: 'Haute',
  URGENT: 'Urgente',
};

function formatValue(field: TaskHistoryField, value: string | null): string {
  if (value === null) return 'Aucun';

  switch (field) {
    case 'STATUS':
      return statusLabels[value] || value;
    case 'PRIORITY':
      return priorityLabels[value] || value;
    case 'DUE_DATE':
      try {
        return new Date(value).toLocaleDateString('fr-FR', {
          day: 'numeric',
          month: 'long',
          year: 'numeric',
        });
      } catch {
        return value;
      }
    case 'DESCRIPTION':
      // Truncate long descriptions
      return value.length > 50 ? value.substring(0, 50) + '...' : value;
    default:
      return value;
  }
}

function formatDate(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return "A l'instant";
  if (diffMins < 60) return `Il y a ${diffMins} min`;
  if (diffHours < 24) return `Il y a ${diffHours}h`;
  if (diffDays < 7) return `Il y a ${diffDays}j`;

  return date.toLocaleDateString('fr-FR', {
    day: 'numeric',
    month: 'short',
    year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined,
  });
}

export default function TaskHistoryTimeline({ taskId }: TaskHistoryTimelineProps) {
  const [history, setHistory] = useState<TaskHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState(false);
  const [selectedField, setSelectedField] = useState<TaskHistoryField | null>(null);
  const [showFilterMenu, setShowFilterMenu] = useState(false);

  useEffect(() => {
    fetchHistory();
  }, [taskId, selectedField]);

  const fetchHistory = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await taskHistoryService.getTaskHistory(taskId, selectedField || undefined);
      setHistory(data);
    } catch (err) {
      setError('Erreur lors du chargement de l\'historique');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const displayedHistory = expanded ? history : history.slice(0, 5);
  const hasMore = history.length > 5;

  const filterOptions: (TaskHistoryField | null)[] = [
    null,
    'TITLE',
    'DESCRIPTION',
    'STATUS',
    'PRIORITY',
    'ASSIGNEE',
    'DUE_DATE',
    'GOAL',
  ];

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <History className="h-4 w-4 text-gray-500 dark:text-gray-400" />
          <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300">
            Historique des modifications
          </h3>
          {history.length > 0 && (
            <span className="rounded-full bg-gray-100 dark:bg-gray-800 px-2 py-0.5 text-xs text-gray-600 dark:text-gray-400">
              {history.length}
            </span>
          )}
        </div>

        {/* Filter dropdown */}
        <div className="relative">
          <button
            onClick={() => setShowFilterMenu(!showFilterMenu)}
            className={cn(
              'flex items-center gap-1.5 rounded-lg px-2 py-1 text-xs font-medium transition-colors',
              selectedField
                ? 'bg-accent/10 text-accent'
                : 'bg-light-hover dark:bg-dark-hover text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-white/10'
            )}
          >
            <Filter className="h-3.5 w-3.5" />
            {selectedField ? fieldLabels[selectedField] : 'Filtrer'}
            <ChevronDown className="h-3 w-3" />
          </button>

          {showFilterMenu && (
            <div className="absolute right-0 top-full mt-1 z-50 min-w-[150px] rounded-lg border border-gray-200 dark:border-white/10 bg-light-card dark:bg-dark-card shadow-lg">
              {filterOptions.map((field) => (
                <button
                  key={field || 'all'}
                  onClick={() => {
                    setSelectedField(field);
                    setShowFilterMenu(false);
                    setExpanded(false);
                  }}
                  className={cn(
                    'w-full flex items-center gap-2 px-3 py-2 text-left text-sm transition-colors first:rounded-t-lg last:rounded-b-lg',
                    selectedField === field
                      ? 'bg-accent/10 text-accent'
                      : 'text-gray-700 dark:text-gray-300 hover:bg-light-hover dark:hover:bg-dark-hover'
                  )}
                >
                  {field ? fieldIcons[field] : <History className="h-4 w-4" />}
                  {field ? fieldLabels[field] : 'Tous'}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-4">
          <div className="h-5 w-5 animate-spin rounded-full border-2 border-accent border-t-transparent" />
        </div>
      ) : error ? (
        <div className="rounded-lg bg-error/10 border border-error/20 px-3 py-2 text-sm text-error">
          {error}
        </div>
      ) : history.length === 0 ? (
        <p className="text-sm text-gray-500 dark:text-gray-400 py-2">
          Aucune modification enregistree
        </p>
      ) : (
        <>
          <div className="relative">
            {/* Timeline line */}
            <div className="absolute left-[15px] top-2 bottom-2 w-0.5 bg-gray-200 dark:bg-gray-700" />

            {/* Timeline items */}
            <div className="flex flex-col gap-3">
              {displayedHistory.map((item) => (
                <div key={item.id} className="relative flex gap-3 pl-1">
                  {/* Timeline dot */}
                  <div className="relative z-10 flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-light-card dark:bg-dark-card border border-gray-200 dark:border-gray-700">
                    <span className="text-gray-500 dark:text-gray-400">
                      {fieldIcons[item.fieldChanged]}
                    </span>
                  </div>

                  {/* Content */}
                  <div className="flex-1 min-w-0 pt-0.5">
                    <div className="flex items-start gap-2">
                      {/* User avatar */}
                      {item.userPhotoUrl ? (
                        <img
                          src={item.userPhotoUrl}
                          alt={item.userName}
                          className="h-5 w-5 rounded-full object-cover flex-shrink-0"
                        />
                      ) : (
                        <div className="h-5 w-5 rounded-full bg-accent/20 flex items-center justify-center flex-shrink-0">
                          <User className="h-3 w-3 text-accent" />
                        </div>
                      )}

                      <div className="flex-1 min-w-0">
                        <p className="text-sm text-gray-700 dark:text-gray-300">
                          <span className="font-medium">{item.userName}</span>
                          {' a modifie '}
                          <span className="font-medium text-accent">
                            {fieldLabels[item.fieldChanged].toLowerCase()}
                          </span>
                        </p>

                        <div className="flex items-center gap-2 mt-1 text-xs">
                          <span className="text-gray-500 dark:text-gray-400 line-through">
                            {formatValue(item.fieldChanged, item.oldValue)}
                          </span>
                          <span className="text-gray-400 dark:text-gray-500">→</span>
                          <span className="text-gray-700 dark:text-gray-300 font-medium">
                            {formatValue(item.fieldChanged, item.newValue)}
                          </span>
                        </div>

                        <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">
                          {formatDate(item.changedAt)}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Show more/less button */}
          {hasMore && (
            <button
              onClick={() => setExpanded(!expanded)}
              className="flex items-center justify-center gap-1 text-sm text-accent hover:text-accent/80 transition-colors py-1"
            >
              {expanded ? 'Voir moins' : `Voir ${history.length - 5} autres modifications`}
              <ChevronDown
                className={cn('h-4 w-4 transition-transform', expanded && 'rotate-180')}
              />
            </button>
          )}
        </>
      )}
    </div>
  );
}
