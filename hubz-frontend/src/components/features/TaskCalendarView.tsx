import { useState, useMemo, useCallback } from 'react';
import {
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  Clock,
  AlertTriangle,
  CheckCircle2,
  Circle,
  Loader2,
} from 'lucide-react';
import Card from '../ui/Card';
import Button from '../ui/Button';
import Modal from '../ui/Modal';
import { cn } from '../../lib/utils';
import type { Task, TaskStatus, TaskPriority } from '../../types/task';
import type { Member } from '../../types/organization';

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const MONTH_NAMES = [
  'Janvier', 'Fevrier', 'Mars', 'Avril', 'Mai', 'Juin',
  'Juillet', 'Aout', 'Septembre', 'Octobre', 'Novembre', 'Decembre',
];

const DAY_NAMES_SHORT = ['Dim', 'Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam'];

const DAY_NAMES_FULL = [
  'Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi',
];

type ViewMode = 'month' | 'week' | 'day';

type ColorMode = 'priority' | 'status';

// ---------------------------------------------------------------------------
// Color helpers
// ---------------------------------------------------------------------------

const PRIORITY_COLORS: Record<TaskPriority, string> = {
  LOW: 'bg-gray-400 dark:bg-gray-500',
  MEDIUM: 'bg-blue-500 dark:bg-blue-400',
  HIGH: 'bg-orange-500 dark:bg-orange-400',
  URGENT: 'bg-red-500 dark:bg-red-400',
};

const PRIORITY_BG_COLORS: Record<TaskPriority, string> = {
  LOW: 'bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700',
  MEDIUM: 'bg-blue-100 dark:bg-blue-900/40 hover:bg-blue-200 dark:hover:bg-blue-900/60',
  HIGH: 'bg-orange-100 dark:bg-orange-900/40 hover:bg-orange-200 dark:hover:bg-orange-900/60',
  URGENT: 'bg-red-100 dark:bg-red-900/40 hover:bg-red-200 dark:hover:bg-red-900/60',
};

const PRIORITY_TEXT_COLORS: Record<TaskPriority, string> = {
  LOW: 'text-gray-700 dark:text-gray-300',
  MEDIUM: 'text-blue-700 dark:text-blue-300',
  HIGH: 'text-orange-700 dark:text-orange-300',
  URGENT: 'text-red-700 dark:text-red-300',
};

const STATUS_COLORS: Record<TaskStatus, string> = {
  TODO: 'bg-gray-400 dark:bg-gray-500',
  IN_PROGRESS: 'bg-accent',
  DONE: 'bg-green-500 dark:bg-green-400',
};

const STATUS_BG_COLORS: Record<TaskStatus, string> = {
  TODO: 'bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700',
  IN_PROGRESS: 'bg-blue-100 dark:bg-blue-900/40 hover:bg-blue-200 dark:hover:bg-blue-900/60',
  DONE: 'bg-green-100 dark:bg-green-900/40 hover:bg-green-200 dark:hover:bg-green-900/60',
};

const STATUS_TEXT_COLORS: Record<TaskStatus, string> = {
  TODO: 'text-gray-700 dark:text-gray-300',
  IN_PROGRESS: 'text-blue-700 dark:text-blue-300',
  DONE: 'text-green-700 dark:text-green-300',
};

const STATUS_ICONS: Record<TaskStatus, React.ComponentType<{ className?: string }>> = {
  TODO: Circle,
  IN_PROGRESS: Loader2,
  DONE: CheckCircle2,
};

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

interface TaskCalendarViewProps {
  tasks: Task[];
  members: Member[];
  onTaskClick: (task: Task) => void;
  onDueDateChange?: (taskId: string, newDueDate: string) => void;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getDate() === b.getDate() &&
    a.getMonth() === b.getMonth() &&
    a.getFullYear() === b.getFullYear()
  );
}

function getStartOfWeek(date: Date): Date {
  const d = new Date(date);
  const day = d.getDay(); // 0=Sun
  d.setDate(d.getDate() - day);
  d.setHours(0, 0, 0, 0);
  return d;
}

function addDays(date: Date, days: number): Date {
  const d = new Date(date);
  d.setDate(d.getDate() + days);
  return d;
}

function formatDateISO(date: Date): string {
  const y = date.getFullYear();
  const m = (date.getMonth() + 1).toString().padStart(2, '0');
  const d = date.getDate().toString().padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function getTasksForDate(tasks: Task[], date: Date): Task[] {
  return tasks.filter((task) => {
    if (!task.dueDate) return false;
    const taskDate = new Date(task.dueDate);
    return isSameDay(taskDate, date);
  });
}

function isOverdue(task: Task): boolean {
  if (!task.dueDate || task.status === 'DONE') return false;
  return new Date(task.dueDate) < new Date();
}

function getTaskColor(task: Task, colorMode: ColorMode): string {
  if (colorMode === 'priority') {
    return PRIORITY_COLORS[task.priority || 'MEDIUM'];
  }
  return STATUS_COLORS[task.status];
}

function getTaskBgColor(task: Task, colorMode: ColorMode): string {
  if (colorMode === 'priority') {
    return PRIORITY_BG_COLORS[task.priority || 'MEDIUM'];
  }
  return STATUS_BG_COLORS[task.status];
}

function getTaskTextColor(task: Task, colorMode: ColorMode): string {
  if (colorMode === 'priority') {
    return PRIORITY_TEXT_COLORS[task.priority || 'MEDIUM'];
  }
  return STATUS_TEXT_COLORS[task.status];
}

// ---------------------------------------------------------------------------
// Main Component
// ---------------------------------------------------------------------------

export default function TaskCalendarView({
  tasks,
  members,
  onTaskClick,
  onDueDateChange,
}: TaskCalendarViewProps) {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [viewMode, setViewMode] = useState<ViewMode>('month');
  const [colorMode, setColorMode] = useState<ColorMode>('priority');
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);

  const today = useMemo(() => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    return d;
  }, []);

  // Filter tasks that have a due date
  const tasksWithDueDate = useMemo(
    () => tasks.filter((t) => t.dueDate !== null),
    [tasks]
  );

  // -- Navigation -----------------------------------------------------------

  const goToPrevious = useCallback(() => {
    setCurrentDate((prev) => {
      const d = new Date(prev);
      if (viewMode === 'month') {
        d.setMonth(d.getMonth() - 1);
      } else if (viewMode === 'week') {
        d.setDate(d.getDate() - 7);
      } else {
        d.setDate(d.getDate() - 1);
      }
      return d;
    });
  }, [viewMode]);

  const goToNext = useCallback(() => {
    setCurrentDate((prev) => {
      const d = new Date(prev);
      if (viewMode === 'month') {
        d.setMonth(d.getMonth() + 1);
      } else if (viewMode === 'week') {
        d.setDate(d.getDate() + 7);
      } else {
        d.setDate(d.getDate() + 1);
      }
      return d;
    });
  }, [viewMode]);

  const goToToday = useCallback(() => {
    setCurrentDate(new Date());
  }, []);

  // -- Header text ----------------------------------------------------------

  const headerText = useMemo(() => {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();

    if (viewMode === 'month') {
      return `${MONTH_NAMES[month]} ${year}`;
    }

    if (viewMode === 'week') {
      const weekStart = getStartOfWeek(currentDate);
      const weekEnd = addDays(weekStart, 6);
      const startMonth = MONTH_NAMES[weekStart.getMonth()];
      const endMonth = MONTH_NAMES[weekEnd.getMonth()];
      const startDay = weekStart.getDate();
      const endDay = weekEnd.getDate();

      if (weekStart.getMonth() === weekEnd.getMonth()) {
        return `${startDay} - ${endDay} ${startMonth} ${year}`;
      }
      const startMonthShort = startMonth.substring(0, 3);
      const endMonthShort = endMonth.substring(0, 3);
      if (weekStart.getFullYear() !== weekEnd.getFullYear()) {
        return `${startDay} ${startMonthShort} ${weekStart.getFullYear()} - ${endDay} ${endMonthShort} ${weekEnd.getFullYear()}`;
      }
      return `${startDay} ${startMonthShort} - ${endDay} ${endMonthShort} ${year}`;
    }

    // Day view
    const dayOfWeek = DAY_NAMES_FULL[currentDate.getDay()];
    const dayNum = currentDate.getDate();
    const monthName = MONTH_NAMES[month];
    return `${dayOfWeek} ${dayNum} ${monthName} ${year}`;
  }, [currentDate, viewMode]);

  // -- Month helpers -------------------------------------------------------

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();
  const firstDayOfMonth = new Date(year, month, 1);
  const lastDayOfMonth = new Date(year, month + 1, 0);
  const startingDayOfWeek = firstDayOfMonth.getDay();
  const daysInMonth = lastDayOfMonth.getDate();

  // -- Week helpers ---------------------------------------------------------

  const weekStart = useMemo(() => getStartOfWeek(currentDate), [currentDate]);
  const weekDays = useMemo(
    () => Array.from({ length: 7 }, (_, i) => addDays(weekStart, i)),
    [weekStart]
  );

  // ========================================================================
  // RENDER
  // ========================================================================

  return (
    <>
      <Card className="p-6 h-full flex flex-col">
        {/* Controls bar */}
        <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <h3 className="text-xl font-bold text-gray-900 dark:text-gray-100">
              {headerText}
            </h3>
            <div className="flex gap-1">
              <button
                onClick={goToPrevious}
                className="rounded-lg p-2 text-gray-600 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800 transition-colors"
                aria-label="Precedent"
              >
                <ChevronLeft className="h-5 w-5" />
              </button>
              <button
                onClick={goToNext}
                className="rounded-lg p-2 text-gray-600 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800 transition-colors"
                aria-label="Suivant"
              >
                <ChevronRight className="h-5 w-5" />
              </button>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {/* Color mode switcher */}
            <div className="flex rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden">
              <button
                onClick={() => setColorMode('priority')}
                className={cn(
                  'px-3 py-1.5 text-sm font-medium transition-colors',
                  colorMode === 'priority'
                    ? 'bg-accent text-white'
                    : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'
                )}
              >
                Priorite
              </button>
              <button
                onClick={() => setColorMode('status')}
                className={cn(
                  'px-3 py-1.5 text-sm font-medium transition-colors',
                  colorMode === 'status'
                    ? 'bg-accent text-white'
                    : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'
                )}
              >
                Statut
              </button>
            </div>

            {/* View switcher */}
            <div className="flex rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden">
              {(['month', 'week', 'day'] as const).map((mode) => {
                const labels: Record<ViewMode, string> = {
                  month: 'Mois',
                  week: 'Semaine',
                  day: 'Jour',
                };
                return (
                  <button
                    key={mode}
                    onClick={() => setViewMode(mode)}
                    className={cn(
                      'px-3 py-1.5 text-sm font-medium transition-colors',
                      viewMode === mode
                        ? 'bg-accent text-white'
                        : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'
                    )}
                  >
                    {labels[mode]}
                  </button>
                );
              })}
            </div>

            <Button variant="secondary" size="sm" onClick={goToToday}>
              Aujourd&apos;hui
            </Button>
          </div>
        </div>

        {/* View content */}
        <div className="flex-1 overflow-auto">
          {viewMode === 'month' && (
            <MonthView
              year={year}
              month={month}
              daysInMonth={daysInMonth}
              startingDayOfWeek={startingDayOfWeek}
              tasks={tasksWithDueDate}
              today={today}
              colorMode={colorMode}
              onDayClick={(date) => setSelectedDate(date)}
              onTaskClick={onTaskClick}
            />
          )}

          {viewMode === 'week' && (
            <WeekView
              weekDays={weekDays}
              tasks={tasksWithDueDate}
              today={today}
              colorMode={colorMode}
              onTaskClick={onTaskClick}
              onDueDateChange={onDueDateChange}
            />
          )}

          {viewMode === 'day' && (
            <DayView
              date={currentDate}
              tasks={tasksWithDueDate}
              today={today}
              colorMode={colorMode}
              members={members}
              onTaskClick={onTaskClick}
            />
          )}
        </div>
      </Card>

      {/* Day Tasks Modal (month view day-click) */}
      {selectedDate && (
        <DayTasksModal
          isOpen={!!selectedDate}
          onClose={() => setSelectedDate(null)}
          date={selectedDate}
          tasks={getTasksForDate(tasksWithDueDate, selectedDate)}
          colorMode={colorMode}
          members={members}
          onTaskClick={(task) => {
            setSelectedDate(null);
            onTaskClick(task);
          }}
        />
      )}
    </>
  );
}

// ===========================================================================
// MONTH VIEW
// ===========================================================================

interface MonthViewProps {
  year: number;
  month: number;
  daysInMonth: number;
  startingDayOfWeek: number;
  tasks: Task[];
  today: Date;
  colorMode: ColorMode;
  onDayClick: (date: Date) => void;
  onTaskClick: (task: Task) => void;
}

function MonthView({
  year,
  month,
  daysInMonth,
  startingDayOfWeek,
  tasks,
  today,
  colorMode,
  onDayClick,
  onTaskClick,
}: MonthViewProps) {
  const isDayToday = (day: number) => {
    return isSameDay(new Date(year, month, day), today);
  };

  const getDayTasks = (day: number) => {
    return getTasksForDate(tasks, new Date(year, month, day));
  };

  return (
    <div className="grid grid-cols-7 gap-2">
      {/* Day names header */}
      {DAY_NAMES_SHORT.map((day) => (
        <div
          key={day}
          className="py-2 text-center text-sm font-semibold text-gray-500 dark:text-gray-400"
        >
          {day}
        </div>
      ))}

      {/* Empty cells before first day */}
      {Array.from({ length: startingDayOfWeek }).map((_, i) => (
        <div key={`empty-${i}`} className="h-24" />
      ))}

      {/* Calendar days */}
      {Array.from({ length: daysInMonth }).map((_, i) => {
        const day = i + 1;
        const dayTasks = getDayTasks(day);
        const isCurrentDay = isDayToday(day);
        const hasOverdueTasks = dayTasks.some(isOverdue);

        return (
          <button
            key={day}
            onClick={() => onDayClick(new Date(year, month, day))}
            className={cn(
              'relative h-24 rounded-lg border p-2 text-left transition-all hover:shadow-md',
              isCurrentDay
                ? 'border-accent bg-accent/5'
                : 'border-gray-200 hover:border-gray-300 dark:border-gray-700 dark:hover:border-gray-600',
              dayTasks.length > 0 && !isCurrentDay && 'bg-blue-50/50 dark:bg-blue-900/10'
            )}
          >
            <span
              className={cn(
                'inline-flex h-6 w-6 items-center justify-center rounded-full text-sm font-medium',
                isCurrentDay
                  ? 'bg-accent text-white'
                  : 'text-gray-700 dark:text-gray-300'
              )}
            >
              {day}
            </span>
            {hasOverdueTasks && (
              <AlertTriangle className="absolute top-2 right-2 h-4 w-4 text-red-500" />
            )}
            {dayTasks.length > 0 && (
              <div className="mt-1 space-y-1">
                {dayTasks.slice(0, 2).map((task) => (
                  <div
                    key={task.id}
                    onClick={(e) => {
                      e.stopPropagation();
                      onTaskClick(task);
                    }}
                    className={cn(
                      'truncate rounded px-1 py-0.5 text-xs font-medium cursor-pointer transition-colors',
                      getTaskBgColor(task, colorMode),
                      getTaskTextColor(task, colorMode)
                    )}
                  >
                    {task.title}
                  </div>
                ))}
                {dayTasks.length > 2 && (
                  <div className="text-xs text-gray-500 dark:text-gray-400">
                    +{dayTasks.length - 2} autre{dayTasks.length - 2 > 1 ? 's' : ''}
                  </div>
                )}
              </div>
            )}
          </button>
        );
      })}
    </div>
  );
}

// ===========================================================================
// WEEK VIEW
// ===========================================================================

interface WeekViewProps {
  weekDays: Date[];
  tasks: Task[];
  today: Date;
  colorMode: ColorMode;
  onTaskClick: (task: Task) => void;
  onDueDateChange?: (taskId: string, newDueDate: string) => void;
}

function WeekView({
  weekDays,
  tasks,
  today,
  colorMode,
  onTaskClick,
  onDueDateChange,
}: WeekViewProps) {
  const [draggedTask, setDraggedTask] = useState<Task | null>(null);

  const handleDragStart = (task: Task, e: React.DragEvent) => {
    setDraggedTask(task);
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', task.id);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  };

  const handleDrop = (date: Date, e: React.DragEvent) => {
    e.preventDefault();
    if (draggedTask && onDueDateChange) {
      onDueDateChange(draggedTask.id, formatDateISO(date));
    }
    setDraggedTask(null);
  };

  const handleDragEnd = () => {
    setDraggedTask(null);
  };

  return (
    <div className="grid grid-cols-7 gap-2 h-full">
      {weekDays.map((day) => {
        const dayTasks = getTasksForDate(tasks, day);
        const isCurrentDay = isSameDay(day, today);
        const hasOverdueTasks = dayTasks.some(isOverdue);

        return (
          <div
            key={day.toISOString()}
            className={cn(
              'flex flex-col rounded-lg border p-3 min-h-[300px] transition-all',
              isCurrentDay
                ? 'border-accent bg-accent/5'
                : 'border-gray-200 dark:border-gray-700',
              draggedTask && 'hover:border-accent hover:bg-accent/10'
            )}
            onDragOver={handleDragOver}
            onDrop={(e) => handleDrop(day, e)}
          >
            {/* Day header */}
            <div className="text-center mb-3 pb-2 border-b border-gray-200 dark:border-gray-700">
              <div className="text-xs font-medium text-gray-500 dark:text-gray-400">
                {DAY_NAMES_SHORT[day.getDay()]}
              </div>
              <div
                className={cn(
                  'mx-auto mt-1 flex h-8 w-8 items-center justify-center rounded-full text-sm font-bold',
                  isCurrentDay
                    ? 'bg-accent text-white'
                    : 'text-gray-900 dark:text-gray-100'
                )}
              >
                {day.getDate()}
              </div>
              {hasOverdueTasks && (
                <AlertTriangle className="mx-auto mt-1 h-4 w-4 text-red-500" />
              )}
            </div>

            {/* Tasks */}
            <div className="flex-1 overflow-y-auto space-y-2">
              {dayTasks.map((task) => {
                const StatusIcon = STATUS_ICONS[task.status];
                return (
                  <div
                    key={task.id}
                    draggable={!!onDueDateChange}
                    onDragStart={(e) => handleDragStart(task, e)}
                    onDragEnd={handleDragEnd}
                    onClick={() => onTaskClick(task)}
                    className={cn(
                      'rounded-lg p-2 cursor-pointer transition-all',
                      getTaskBgColor(task, colorMode),
                      onDueDateChange && 'cursor-move',
                      isOverdue(task) && 'ring-1 ring-red-500'
                    )}
                  >
                    <div className="flex items-start gap-2">
                      <div
                        className={cn(
                          'mt-0.5 h-2 w-2 rounded-full flex-shrink-0',
                          getTaskColor(task, colorMode)
                        )}
                      />
                      <div className="flex-1 min-w-0">
                        <p
                          className={cn(
                            'text-xs font-medium truncate',
                            getTaskTextColor(task, colorMode)
                          )}
                        >
                          {task.title}
                        </p>
                        <div className="flex items-center gap-1 mt-1">
                          <StatusIcon className="h-3 w-3 text-gray-400" />
                          {isOverdue(task) && (
                            <span className="text-[10px] text-red-500 font-medium">
                              En retard
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
              {dayTasks.length === 0 && (
                <p className="text-xs text-gray-400 dark:text-gray-500 text-center py-4">
                  Aucune tache
                </p>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}

// ===========================================================================
// DAY VIEW
// ===========================================================================

interface DayViewProps {
  date: Date;
  tasks: Task[];
  today: Date;
  colorMode: ColorMode;
  members: Member[];
  onTaskClick: (task: Task) => void;
}

function DayView({
  date,
  tasks,
  today,
  colorMode,
  members,
  onTaskClick,
}: DayViewProps) {
  const dayTasks = useMemo(() => getTasksForDate(tasks, date), [tasks, date]);
  const isCurrentDay = isSameDay(date, today);

  // Group tasks by status
  const tasksByStatus = useMemo(() => {
    const grouped: Record<TaskStatus, Task[]> = {
      TODO: [],
      IN_PROGRESS: [],
      DONE: [],
    };
    dayTasks.forEach((task) => {
      grouped[task.status].push(task);
    });
    return grouped;
  }, [dayTasks]);

  const getMemberName = (assigneeId: string | null) => {
    if (!assigneeId) return null;
    const member = members.find((m) => m.userId === assigneeId);
    return member ? `${member.firstName} ${member.lastName}` : null;
  };

  return (
    <div className="h-full">
      {/* Day header */}
      <div
        className={cn(
          'mb-4 rounded-lg px-4 py-3',
          isCurrentDay
            ? 'bg-accent/10 border border-accent/20'
            : 'bg-gray-50 dark:bg-gray-800/50'
        )}
      >
        <div className="text-sm font-medium text-gray-500 dark:text-gray-400">
          {DAY_NAMES_FULL[date.getDay()]}
        </div>
        <div
          className={cn(
            'text-2xl font-bold',
            isCurrentDay ? 'text-accent' : 'text-gray-900 dark:text-gray-100'
          )}
        >
          {date.getDate()} {MONTH_NAMES[date.getMonth()]} {date.getFullYear()}
        </div>
        <div className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          {dayTasks.length} tache{dayTasks.length !== 1 ? 's' : ''} prevue{dayTasks.length !== 1 ? 's' : ''}
        </div>
      </div>

      {dayTasks.length === 0 ? (
        <div className="py-12 text-center">
          <CalendarIcon className="mx-auto h-12 w-12 text-gray-400" />
          <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
            Aucune tache prevue pour cette journee
          </p>
        </div>
      ) : (
        <div className="grid gap-6 md:grid-cols-3">
          {(['TODO', 'IN_PROGRESS', 'DONE'] as TaskStatus[]).map((status) => {
            const StatusIcon = STATUS_ICONS[status];
            const statusLabels: Record<TaskStatus, string> = {
              TODO: 'A faire',
              IN_PROGRESS: 'En cours',
              DONE: 'Termine',
            };
            return (
              <div key={status}>
                <div className="flex items-center gap-2 mb-3">
                  <StatusIcon className="h-4 w-4 text-gray-500" />
                  <h4 className="text-sm font-semibold text-gray-700 dark:text-gray-300">
                    {statusLabels[status]}
                  </h4>
                  <span className="text-xs text-gray-400">
                    ({tasksByStatus[status].length})
                  </span>
                </div>
                <div className="space-y-2">
                  {tasksByStatus[status].map((task) => {
                    const assigneeName = getMemberName(task.assigneeId);
                    return (
                      <button
                        key={task.id}
                        onClick={() => onTaskClick(task)}
                        className={cn(
                          'w-full rounded-lg border p-3 text-left transition-all hover:shadow-md',
                          getTaskBgColor(task, colorMode),
                          isOverdue(task)
                            ? 'border-red-300 dark:border-red-700'
                            : 'border-transparent'
                        )}
                      >
                        <div className="flex items-start gap-2">
                          <div
                            className={cn(
                              'mt-1 h-2 w-2 rounded-full flex-shrink-0',
                              getTaskColor(task, colorMode)
                            )}
                          />
                          <div className="flex-1 min-w-0">
                            <p
                              className={cn(
                                'font-medium',
                                getTaskTextColor(task, colorMode)
                              )}
                            >
                              {task.title}
                            </p>
                            {task.description && (
                              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400 line-clamp-2">
                                {task.description}
                              </p>
                            )}
                            <div className="mt-2 flex items-center gap-3 text-xs text-gray-500 dark:text-gray-400">
                              {assigneeName && (
                                <span>{assigneeName}</span>
                              )}
                              {isOverdue(task) && (
                                <span className="flex items-center gap-1 text-red-500">
                                  <AlertTriangle className="h-3 w-3" />
                                  En retard
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                      </button>
                    );
                  })}
                  {tasksByStatus[status].length === 0 && (
                    <p className="text-xs text-gray-400 dark:text-gray-500 text-center py-4">
                      Aucune tache
                    </p>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

// ===========================================================================
// DAY TASKS MODAL (used by month view when clicking a day)
// ===========================================================================

interface DayTasksModalProps {
  isOpen: boolean;
  onClose: () => void;
  date: Date;
  tasks: Task[];
  colorMode: ColorMode;
  members: Member[];
  onTaskClick: (task: Task) => void;
}

function DayTasksModal({
  isOpen,
  onClose,
  date,
  tasks,
  colorMode,
  members,
  onTaskClick,
}: DayTasksModalProps) {
  const getMemberName = (assigneeId: string | null) => {
    if (!assigneeId) return null;
    const member = members.find((m) => m.userId === assigneeId);
    return member ? `${member.firstName} ${member.lastName}` : null;
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={date.toLocaleDateString('fr-FR', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      })}
    >
      <div className="space-y-4">
        {tasks.length === 0 ? (
          <div className="py-8 text-center">
            <CalendarIcon className="mx-auto h-12 w-12 text-gray-400" />
            <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
              Aucune tache prevue pour cette journee
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {tasks
              .sort((a, b) => {
                // Sort by status (TODO first, then IN_PROGRESS, then DONE)
                const statusOrder: Record<TaskStatus, number> = {
                  TODO: 0,
                  IN_PROGRESS: 1,
                  DONE: 2,
                };
                return statusOrder[a.status] - statusOrder[b.status];
              })
              .map((task) => {
                const StatusIcon = STATUS_ICONS[task.status];
                const assigneeName = getMemberName(task.assigneeId);

                return (
                  <button
                    key={task.id}
                    onClick={() => onTaskClick(task)}
                    className={cn(
                      'w-full rounded-lg border p-4 text-left transition-all hover:shadow-md',
                      getTaskBgColor(task, colorMode),
                      isOverdue(task)
                        ? 'border-red-300 dark:border-red-700'
                        : 'border-gray-200 dark:border-gray-700 hover:border-accent'
                    )}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex items-start gap-3 flex-1 min-w-0">
                        <div
                          className={cn(
                            'mt-1 h-3 w-3 rounded-full flex-shrink-0',
                            getTaskColor(task, colorMode)
                          )}
                        />
                        <div className="flex-1 min-w-0">
                          <h4
                            className={cn(
                              'font-semibold',
                              getTaskTextColor(task, colorMode)
                            )}
                          >
                            {task.title}
                          </h4>
                          {task.description && (
                            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400 line-clamp-2">
                              {task.description}
                            </p>
                          )}
                        </div>
                      </div>
                    </div>
                    <div className="mt-3 flex items-center gap-4 text-xs text-gray-500 dark:text-gray-400">
                      <span className="flex items-center gap-1">
                        <StatusIcon className="h-3 w-3" />
                        {task.status === 'TODO' && 'A faire'}
                        {task.status === 'IN_PROGRESS' && 'En cours'}
                        {task.status === 'DONE' && 'Termine'}
                      </span>
                      {assigneeName && <span>{assigneeName}</span>}
                      {isOverdue(task) && (
                        <span className="flex items-center gap-1 text-red-500">
                          <AlertTriangle className="h-3 w-3" />
                          En retard
                        </span>
                      )}
                    </div>
                  </button>
                );
              })}
          </div>
        )}
        <Button onClick={onClose} className="w-full">
          Fermer
        </Button>
      </div>
    </Modal>
  );
}
