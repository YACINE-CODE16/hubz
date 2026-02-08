import { useState, useMemo } from 'react';
import {
  Calendar,
  AlertTriangle,
  UserCircle2,
  ChevronUp,
  ChevronDown,
  ChevronsUpDown,
  CheckCircle2,
  Circle,
  Clock,
  Search,
  X,
} from 'lucide-react';
import type { Task, TaskStatus, TaskPriority } from '../../types/task';
import type { Member } from '../../types/organization';
import type { Tag } from '../../types/tag';
import { cn } from '../../lib/utils';
import TagChip from '../ui/TagChip';

const priorityConfig: Record<TaskPriority, { label: string; className: string; order: number }> = {
  LOW: { label: 'Basse', className: 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400', order: 1 },
  MEDIUM: { label: 'Moyenne', className: 'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-400', order: 2 },
  HIGH: { label: 'Haute', className: 'bg-orange-100 text-orange-700 dark:bg-orange-900/40 dark:text-orange-400', order: 3 },
  URGENT: { label: 'Urgente', className: 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-400', order: 4 },
};

const statusConfig: Record<TaskStatus, { label: string; className: string; icon: typeof CheckCircle2 }> = {
  TODO: { label: 'A faire', className: 'text-gray-500 dark:text-gray-400', icon: Circle },
  IN_PROGRESS: { label: 'En cours', className: 'text-blue-500 dark:text-blue-400', icon: Clock },
  DONE: { label: 'Termine', className: 'text-green-500 dark:text-green-400', icon: CheckCircle2 },
};

type SortField = 'title' | 'status' | 'priority' | 'dueDate' | 'assignee' | 'createdAt';
type SortDirection = 'asc' | 'desc';

interface TaskListViewProps {
  tasks: Task[];
  members: Member[];
  availableTags?: Tag[];
  onTaskClick: (task: Task) => void;
  onStatusChange: (taskId: string, status: TaskStatus) => void;
}

export default function TaskListView({
  tasks,
  members,
  availableTags = [],
  onTaskClick,
  onStatusChange,
}: TaskListViewProps) {
  const [sortField, setSortField] = useState<SortField>('createdAt');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
  const [statusFilter, setStatusFilter] = useState<TaskStatus | 'ALL'>('ALL');
  const [priorityFilter, setPriorityFilter] = useState<TaskPriority | 'ALL'>('ALL');
  const [tagFilter, setTagFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  };

  const filteredAndSortedTasks = useMemo(() => {
    let filtered = [...tasks];

    // Apply search filter
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        (t) =>
          t.title.toLowerCase().includes(query) ||
          (t.description && t.description.toLowerCase().includes(query)) ||
          t.tags?.some((tag) => tag.name.toLowerCase().includes(query))
      );
    }

    // Apply status filter
    if (statusFilter !== 'ALL') {
      filtered = filtered.filter((t) => t.status === statusFilter);
    }

    // Apply priority filter
    if (priorityFilter !== 'ALL') {
      filtered = filtered.filter((t) => t.priority === priorityFilter);
    }

    // Apply tag filter
    if (tagFilter !== 'ALL') {
      filtered = filtered.filter((t) => t.tags?.some((tag) => tag.id === tagFilter));
    }

    // Apply sorting
    filtered.sort((a, b) => {
      let comparison = 0;

      switch (sortField) {
        case 'title':
          comparison = a.title.localeCompare(b.title);
          break;
        case 'status':
          const statusOrder = { TODO: 1, IN_PROGRESS: 2, DONE: 3 };
          comparison = statusOrder[a.status] - statusOrder[b.status];
          break;
        case 'priority':
          const aPriority = a.priority ? priorityConfig[a.priority].order : 0;
          const bPriority = b.priority ? priorityConfig[b.priority].order : 0;
          comparison = aPriority - bPriority;
          break;
        case 'dueDate':
          if (!a.dueDate && !b.dueDate) comparison = 0;
          else if (!a.dueDate) comparison = 1;
          else if (!b.dueDate) comparison = -1;
          else comparison = new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
          break;
        case 'assignee':
          const aAssignee = a.assigneeId ? members.find((m) => m.userId === a.assigneeId) : null;
          const bAssignee = b.assigneeId ? members.find((m) => m.userId === b.assigneeId) : null;
          const aName = aAssignee ? `${aAssignee.firstName} ${aAssignee.lastName}` : '';
          const bName = bAssignee ? `${bAssignee.firstName} ${bAssignee.lastName}` : '';
          comparison = aName.localeCompare(bName);
          break;
        case 'createdAt':
          comparison = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
          break;
      }

      return sortDirection === 'asc' ? comparison : -comparison;
    });

    return filtered;
  }, [tasks, sortField, sortDirection, statusFilter, priorityFilter, tagFilter, searchQuery, members]);

  const SortIcon = ({ field }: { field: SortField }) => {
    if (sortField !== field) {
      return <ChevronsUpDown className="h-4 w-4 text-gray-400" />;
    }
    return sortDirection === 'asc' ? (
      <ChevronUp className="h-4 w-4 text-accent" />
    ) : (
      <ChevronDown className="h-4 w-4 text-accent" />
    );
  };

  const formatDate = (date: string) =>
    new Date(date).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  return (
    <div className="flex h-full flex-col">
      {/* Search and Filters */}
      <div className="flex flex-col gap-3 px-4 py-3 border-b border-gray-200 dark:border-gray-700 sm:px-6">
        {/* Search input */}
        <div className="relative w-full sm:max-w-md">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Rechercher..."
            className="w-full rounded-lg border border-gray-200 bg-white py-2 pl-9 pr-8 text-sm focus:border-accent focus:outline-none dark:border-gray-700 dark:bg-dark-card sm:py-1.5"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-2 top-1/2 -translate-y-1/2 rounded-full p-0.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-800"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </div>

        {/* Filters row */}
        <div className="flex flex-wrap items-center gap-2">
          <div className="flex items-center gap-1.5">
            <label className="text-xs text-gray-600 dark:text-gray-400 sm:text-sm">Statut:</label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as TaskStatus | 'ALL')}
              className="rounded-lg border border-gray-200 bg-white px-2 py-1.5 text-xs dark:border-gray-700 dark:bg-dark-card sm:px-3 sm:text-sm"
            >
              <option value="ALL">Tous</option>
              <option value="TODO">A faire</option>
              <option value="IN_PROGRESS">En cours</option>
              <option value="DONE">Termine</option>
            </select>
          </div>
          <div className="flex items-center gap-1.5">
            <label className="text-xs text-gray-600 dark:text-gray-400 sm:text-sm">Priorite:</label>
            <select
              value={priorityFilter}
              onChange={(e) => setPriorityFilter(e.target.value as TaskPriority | 'ALL')}
              className="rounded-lg border border-gray-200 bg-white px-2 py-1.5 text-xs dark:border-gray-700 dark:bg-dark-card sm:px-3 sm:text-sm"
            >
              <option value="ALL">Toutes</option>
              <option value="LOW">Basse</option>
              <option value="MEDIUM">Moyenne</option>
              <option value="HIGH">Haute</option>
              <option value="URGENT">Urgente</option>
            </select>
          </div>
          {availableTags.length > 0 && (
            <div className="flex items-center gap-1.5">
              <label className="text-xs text-gray-600 dark:text-gray-400 sm:text-sm">Tag:</label>
              <select
                value={tagFilter}
                onChange={(e) => setTagFilter(e.target.value)}
                className="rounded-lg border border-gray-200 bg-white px-2 py-1.5 text-xs dark:border-gray-700 dark:bg-dark-card sm:px-3 sm:text-sm"
              >
                <option value="ALL">Tous</option>
                {availableTags.map((tag) => (
                  <option key={tag.id} value={tag.id}>
                    {tag.name}
                  </option>
                ))}
              </select>
            </div>
          )}
          <div className="ml-auto text-xs text-gray-500 dark:text-gray-400 sm:text-sm">
            {filteredAndSortedTasks.length} tache{filteredAndSortedTasks.length !== 1 ? 's' : ''}
          </div>
        </div>
      </div>

      {/* Mobile: Card layout */}
      <div className="flex-1 overflow-auto md:hidden">
        {filteredAndSortedTasks.length === 0 ? (
          <div className="px-4 py-12 text-center text-gray-500 dark:text-gray-400">
            {searchQuery ? 'Aucune tache ne correspond a votre recherche' : 'Aucune tache trouvee'}
          </div>
        ) : (
          <div className="space-y-2 p-4">
            {filteredAndSortedTasks.map((task) => {
              const isOverdue =
                task.dueDate &&
                new Date(task.dueDate) < today &&
                task.status !== 'DONE';
              const priority = task.priority ? priorityConfig[task.priority] : null;
              const status = statusConfig[task.status];
              const StatusIcon = status.icon;
              const assignee = task.assigneeId
                ? members.find((m) => m.userId === task.assigneeId)
                : null;

              return (
                <div
                  key={task.id}
                  className={cn(
                    'rounded-xl border border-gray-200 dark:border-gray-700 p-3 transition-colors active:bg-light-hover dark:active:bg-dark-hover',
                    task.status === 'DONE' && 'opacity-60',
                    isOverdue && 'border-error/40',
                  )}
                  onClick={() => onTaskClick(task)}
                >
                  <div className="flex items-start gap-3">
                    {/* Quick complete */}
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onStatusChange(task.id, task.status === 'DONE' ? 'TODO' : 'DONE');
                      }}
                      className={cn(
                        'mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border transition-colors',
                        task.status === 'DONE'
                          ? 'border-green-500 bg-green-500 text-white'
                          : 'border-gray-300 dark:border-gray-600',
                      )}
                    >
                      {task.status === 'DONE' && <CheckCircle2 className="h-3 w-3" />}
                    </button>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className={cn(
                          'text-sm font-medium text-gray-900 dark:text-gray-100 truncate',
                          task.status === 'DONE' && 'line-through',
                        )}>
                          {task.title}
                        </span>
                        {isOverdue && <AlertTriangle className="h-3.5 w-3.5 shrink-0 text-red-500" />}
                      </div>

                      {/* Meta row */}
                      <div className="mt-1.5 flex flex-wrap items-center gap-1.5">
                        <div className={cn('flex items-center gap-1 text-xs', status.className)}>
                          <StatusIcon className="h-3 w-3" />
                          <span>{status.label}</span>
                        </div>
                        {priority && (
                          <span className={cn('rounded-full px-2 py-0.5 text-xs font-medium', priority.className)}>
                            {priority.label}
                          </span>
                        )}
                        {task.dueDate && (
                          <span className={cn(
                            'inline-flex items-center gap-1 text-xs',
                            isOverdue ? 'font-medium text-red-500' : 'text-gray-500 dark:text-gray-400',
                          )}>
                            <Calendar className="h-3 w-3" />
                            {formatDate(task.dueDate)}
                          </span>
                        )}
                        {assignee && (
                          <span className="inline-flex items-center gap-1 text-xs text-gray-500 dark:text-gray-400">
                            <UserCircle2 className="h-3 w-3" />
                            {assignee.firstName}
                          </span>
                        )}
                      </div>

                      {/* Tags */}
                      {task.tags && task.tags.length > 0 && (
                        <div className="mt-1.5 flex flex-wrap gap-1">
                          {task.tags.slice(0, 3).map((tag) => (
                            <TagChip key={tag.id} tag={tag} size="sm" />
                          ))}
                          {task.tags.length > 3 && (
                            <span className="text-xs text-gray-400">+{task.tags.length - 3}</span>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Desktop: Table layout */}
      <div className="hidden flex-1 overflow-auto md:block">
        <table className="w-full">
          <thead className="sticky top-0 bg-light-card dark:bg-dark-card border-b border-gray-200 dark:border-gray-700">
            <tr>
              <th className="w-10 px-4 py-3"></th>
              <th className="px-4 py-3 text-left">
                <button
                  onClick={() => handleSort('title')}
                  className="flex items-center gap-1 text-sm font-medium text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200"
                >
                  Titre
                  <SortIcon field="title" />
                </button>
              </th>
              <th className="px-4 py-3 text-left">
                <span className="text-sm font-medium text-gray-600 dark:text-gray-400">Tags</span>
              </th>
              <th className="px-4 py-3 text-left">
                <button
                  onClick={() => handleSort('status')}
                  className="flex items-center gap-1 text-sm font-medium text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200"
                >
                  Statut
                  <SortIcon field="status" />
                </button>
              </th>
              <th className="px-4 py-3 text-left">
                <button
                  onClick={() => handleSort('priority')}
                  className="flex items-center gap-1 text-sm font-medium text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200"
                >
                  Priorite
                  <SortIcon field="priority" />
                </button>
              </th>
              <th className="px-4 py-3 text-left">
                <button
                  onClick={() => handleSort('dueDate')}
                  className="flex items-center gap-1 text-sm font-medium text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200"
                >
                  Echeance
                  <SortIcon field="dueDate" />
                </button>
              </th>
              <th className="hidden px-4 py-3 text-left lg:table-cell">
                <button
                  onClick={() => handleSort('assignee')}
                  className="flex items-center gap-1 text-sm font-medium text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200"
                >
                  Assigne
                  <SortIcon field="assignee" />
                </button>
              </th>
              <th className="hidden px-4 py-3 text-left xl:table-cell">
                <button
                  onClick={() => handleSort('createdAt')}
                  className="flex items-center gap-1 text-sm font-medium text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200"
                >
                  Cree le
                  <SortIcon field="createdAt" />
                </button>
              </th>
            </tr>
          </thead>
          <tbody>
            {filteredAndSortedTasks.length === 0 ? (
              <tr>
                <td colSpan={8} className="px-4 py-12 text-center text-gray-500 dark:text-gray-400">
                  {searchQuery ? 'Aucune tache ne correspond a votre recherche' : 'Aucune tache trouvee'}
                </td>
              </tr>
            ) : (
              filteredAndSortedTasks.map((task) => {
                const isOverdue =
                  task.dueDate &&
                  new Date(task.dueDate) < today &&
                  task.status !== 'DONE';
                const priority = task.priority ? priorityConfig[task.priority] : null;
                const status = statusConfig[task.status];
                const StatusIcon = status.icon;
                const assignee = task.assigneeId
                  ? members.find((m) => m.userId === task.assigneeId)
                  : null;

                return (
                  <tr
                    key={task.id}
                    className={cn(
                      'border-b border-gray-100 dark:border-gray-800 hover:bg-light-hover dark:hover:bg-dark-hover cursor-pointer transition-colors',
                      task.status === 'DONE' && 'opacity-60'
                    )}
                    onClick={() => onTaskClick(task)}
                  >
                    {/* Quick complete button */}
                    <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                      <button
                        onClick={() =>
                          onStatusChange(task.id, task.status === 'DONE' ? 'TODO' : 'DONE')
                        }
                        className={cn(
                          'flex h-5 w-5 items-center justify-center rounded-full border transition-colors',
                          task.status === 'DONE'
                            ? 'border-green-500 bg-green-500 text-white'
                            : 'border-gray-300 dark:border-gray-600 hover:border-green-500 hover:bg-green-50 dark:hover:bg-green-900/20'
                        )}
                        aria-label={task.status === 'DONE' ? 'Marquer comme a faire' : 'Marquer comme termine'}
                      >
                        {task.status === 'DONE' && <CheckCircle2 className="h-3 w-3" />}
                      </button>
                    </td>

                    {/* Title */}
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <span
                          className={cn(
                            'text-sm font-medium text-gray-900 dark:text-gray-100',
                            task.status === 'DONE' && 'line-through'
                          )}
                        >
                          {task.title}
                        </span>
                        {isOverdue && (
                          <AlertTriangle className="h-4 w-4 text-red-500" aria-label="En retard" />
                        )}
                      </div>
                      {task.description && (
                        <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400 line-clamp-1">
                          {task.description}
                        </p>
                      )}
                    </td>

                    {/* Tags */}
                    <td className="px-4 py-3">
                      {task.tags && task.tags.length > 0 ? (
                        <div className="flex flex-wrap gap-1">
                          {task.tags.slice(0, 3).map((tag) => (
                            <TagChip key={tag.id} tag={tag} size="sm" />
                          ))}
                          {task.tags.length > 3 && (
                            <span className="text-xs text-gray-400">+{task.tags.length - 3}</span>
                          )}
                        </div>
                      ) : (
                        <span className="text-xs text-gray-400">-</span>
                      )}
                    </td>

                    {/* Status */}
                    <td className="px-4 py-3">
                      <div className={cn('flex items-center gap-1.5 text-sm', status.className)}>
                        <StatusIcon className="h-4 w-4" />
                        {status.label}
                      </div>
                    </td>

                    {/* Priority */}
                    <td className="px-4 py-3">
                      {priority ? (
                        <span
                          className={cn(
                            'inline-block rounded-full px-2.5 py-0.5 text-xs font-medium',
                            priority.className
                          )}
                        >
                          {priority.label}
                        </span>
                      ) : (
                        <span className="text-xs text-gray-400">-</span>
                      )}
                    </td>

                    {/* Due Date */}
                    <td className="px-4 py-3">
                      {task.dueDate ? (
                        <span
                          className={cn(
                            'inline-flex items-center gap-1 text-sm',
                            isOverdue
                              ? 'font-medium text-red-500'
                              : 'text-gray-600 dark:text-gray-400'
                          )}
                        >
                          <Calendar className="h-3.5 w-3.5" />
                          {formatDate(task.dueDate)}
                        </span>
                      ) : (
                        <span className="text-xs text-gray-400">-</span>
                      )}
                    </td>

                    {/* Assignee - hidden on medium screens */}
                    <td className="hidden px-4 py-3 lg:table-cell">
                      {assignee ? (
                        <span className="inline-flex items-center gap-1.5 text-sm text-gray-600 dark:text-gray-400">
                          <UserCircle2 className="h-4 w-4" />
                          {assignee.firstName} {assignee.lastName}
                        </span>
                      ) : (
                        <span className="text-xs text-gray-400">Non assigne</span>
                      )}
                    </td>

                    {/* Created At - hidden on medium/large */}
                    <td className="hidden px-4 py-3 text-sm text-gray-500 dark:text-gray-400 xl:table-cell">
                      {formatDate(task.createdAt)}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
